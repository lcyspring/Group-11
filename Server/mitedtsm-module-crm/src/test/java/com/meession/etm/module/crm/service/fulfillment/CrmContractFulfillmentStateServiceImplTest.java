package com.meession.etm.module.crm.service.fulfillment;

import com.meession.etm.framework.common.exception.ServiceException;
import com.meession.etm.module.crm.dal.dataobject.contract.CrmContractDO;
import com.meession.etm.module.crm.dal.dataobject.contract.CrmContractProductDO;
import com.meession.etm.module.crm.dal.dataobject.contract.CrmContractSigningDO;
import com.meession.etm.module.crm.dal.dataobject.fulfillment.CrmContractFulfillmentDO;
import com.meession.etm.module.crm.dal.dataobject.fulfillment.CrmErpCustomerMappingDO;
import com.meession.etm.module.crm.dal.dataobject.fulfillment.CrmErpProductMappingDO;
import com.meession.etm.module.crm.dal.mysql.contract.CrmContractMapper;
import com.meession.etm.module.crm.dal.mysql.contract.CrmContractProductMapper;
import com.meession.etm.module.crm.dal.mysql.contract.CrmContractSigningMapper;
import com.meession.etm.module.crm.dal.mysql.fulfillment.CrmContractFulfillmentMapper;
import com.meession.etm.module.crm.dal.mysql.fulfillment.CrmErpCustomerMappingMapper;
import com.meession.etm.module.crm.dal.mysql.fulfillment.CrmErpProductMappingMapper;
import com.meession.etm.module.crm.enums.common.CrmAuditStatusEnum;
import com.meession.etm.module.crm.framework.fulfillment.CrmErpFulfillmentProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

import static com.meession.etm.module.crm.enums.ErrorCodeConstants.ERP_CUSTOMER_MAPPING_NOT_EXISTS;
import static com.meession.etm.module.crm.enums.contract.CrmContractLifecycleEnums.SIGNED;
import static com.meession.etm.module.crm.enums.fulfillment.CrmContractFulfillmentStatus.FAILED;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CrmContractFulfillmentStateServiceImplTest {

    @Mock private CrmContractMapper contractMapper;
    @Mock private CrmContractProductMapper productMapper;
    @Mock private CrmContractSigningMapper signingMapper;
    @Mock private CrmErpCustomerMappingMapper customerMappingMapper;
    @Mock private CrmErpProductMappingMapper productMappingMapper;
    @Mock private CrmContractFulfillmentMapper fulfillmentMapper;
    @Mock private CrmErpFulfillmentProperties properties;
    @InjectMocks private CrmContractFulfillmentStateServiceImpl service;

    @BeforeEach
    void setUpPolicy() {
        lenient().when(properties.isEnabled()).thenReturn(true);
        lenient().when(properties.getSourceSystem()).thenReturn("CRM");
        lenient().when(properties.getSourceType()).thenReturn("CONTRACT");
        lenient().when(properties.getErpCurrency()).thenReturn("CNY");
        lenient().when(properties.getCurrencyMode()).thenReturn(
                CrmErpFulfillmentProperties.CurrencyMode.CONVERT_TO_ERP_CURRENCY);
        lenient().when(properties.supportsSourceCurrency(any())).thenReturn(true);
        lenient().when(properties.getAmountScale()).thenReturn(6);
        lenient().when(properties.getRoundingMode()).thenReturn(RoundingMode.HALF_UP);
        lenient().when(properties.getTotalTolerance()).thenReturn(new BigDecimal("0.010000"));
    }

    @Test
    void prepareFreezesMappedConvertedRequest() {
        stubContractAndSigning();
        when(fulfillmentMapper.selectByContractIdForUpdate(7L)).thenReturn(null);
        when(productMapper.selectListByContractId(7L)).thenReturn(List.of(product()));
        when(customerMappingMapper.selectByCrmCustomerId(2L))
                .thenReturn(new CrmErpCustomerMappingDO().setCrmCustomerId(2L).setErpCustomerId(20L));
        when(productMappingMapper.selectByCrmProductIds(Set.of(3L)))
                .thenReturn(List.of(new CrmErpProductMappingDO().setCrmProductId(3L).setErpProductId(30L)));
        doAnswer(invocation -> {
            ((CrmContractFulfillmentDO) invocation.getArgument(0)).setId(99L);
            return 1;
        }).when(fulfillmentMapper).insert(any(CrmContractFulfillmentDO.class));

        var result = service.prepare(7L);

        assertFalse(result.alreadyCreated());
        assertEquals(20L, result.request().getCustomerId());
        assertEquals(30L, result.request().getItems().get(0).getProductId());
        assertEquals(new BigDecimal("72.000000"), result.request().getItems().get(0).getProductPrice());
        assertEquals(new BigDecimal("154.584000"), result.request().getExpectedTotalPrice());
        assertEquals(64, result.request().getRequestHash().length());
        ArgumentCaptor<CrmContractFulfillmentDO> captor = ArgumentCaptor.forClass(CrmContractFulfillmentDO.class);
        verify(fulfillmentMapper).insert(captor.capture());
        assertTrue(captor.getValue().getRequestSnapshot().contains("CRM-CONTRACT-7-V2"));
    }

    @Test
    void prepareRejectsMissingCustomerMappingBeforeCallingErp() {
        stubContractAndSigning();
        when(fulfillmentMapper.selectByContractIdForUpdate(7L)).thenReturn(null);
        when(productMapper.selectListByContractId(7L)).thenReturn(List.of(product()));

        ServiceException ex = assertThrows(ServiceException.class, () -> service.prepare(7L));

        assertEquals(ERP_CUSTOMER_MAPPING_NOT_EXISTS.getCode(), ex.getCode());
        verify(fulfillmentMapper, never()).insert(any(CrmContractFulfillmentDO.class));
    }

    @Test
    void failedRetryUsesFrozenSnapshotAndIncrementsAttempt() {
        stubContractAndSigning();
        var frozen = new com.meession.etm.module.erp.api.sale.dto.ErpSaleOrderCreateReqDTO()
                .setSourceSystem("CRM").setSourceType("CONTRACT").setSourceId(7L)
                .setRequestId("CRM-CONTRACT-7-V2").setRequestHash("a".repeat(64))
                .setCustomerId(20L).setOrderTime(LocalDateTime.now()).setDiscountPercent(BigDecimal.ZERO)
                .setCurrencyCode("CNY").setSourceCurrencyCode("USD")
                .setExchangeRateToOrderCurrency(new BigDecimal("7.2")).setSourceGrossAmount(BigDecimal.ONE)
                .setExpectedTotalPrice(BigDecimal.ONE).setItems(List.of(
                        new com.meession.etm.module.erp.api.sale.dto.ErpSaleOrderCreateReqDTO.Item()
                                .setProductId(30L).setProductPrice(BigDecimal.ONE).setCount(BigDecimal.ONE)
                                .setTaxPercent(BigDecimal.ZERO)));
        CrmContractFulfillmentDO existing = new CrmContractFulfillmentDO().setId(9L).setContractId(7L)
                .setStatus(FAILED).setAttemptCount(2).setRequestId(frozen.getRequestId())
                .setRequestHash(frozen.getRequestHash())
                .setRequestSnapshot(com.meession.etm.framework.common.util.json.JsonUtils.toJsonString(frozen));
        when(fulfillmentMapper.selectByContractIdForUpdate(7L)).thenReturn(existing);

        var result = service.prepare(7L);

        assertEquals(3, result.fulfillment().getAttemptCount());
        assertEquals(20L, result.request().getCustomerId());
        verifyNoInteractions(customerMappingMapper, productMappingMapper);
        verify(fulfillmentMapper).markRetrying(eq(9L), eq(3), any());
    }

    private void stubContractAndSigning() {
        when(contractMapper.selectByIdForUpdate(7L)).thenReturn(contract());
        when(signingMapper.selectByContractId(7L)).thenReturn(new CrmContractSigningDO().setContractId(7L)
                .setContractVersion(2).setStatus(SIGNED).setSignedTime(LocalDateTime.of(2026, 7, 16, 8, 0)));
    }

    private static CrmContractDO contract() {
        return new CrmContractDO().setId(7L).setName("美元合同").setNo("HT7").setCustomerId(2L)
                .setOwnerUserId(1L).setAuditStatus(CrmAuditStatusEnum.APPROVE.getStatus())
                .setDiscountPercent(new BigDecimal("5.000000")).setCurrencyCode("USD")
                .setBaseCurrencyCode("CNY").setExchangeRateToBase(new BigDecimal("7.200000"))
                .setGrossAmount(new BigDecimal("21.470000")).setBaseGrossAmount(new BigDecimal("154.584000"));
    }

    private static CrmContractProductDO product() {
        return new CrmContractProductDO().setId(1L).setContractId(7L).setProductId(3L)
                .setProductNameSnapshot("产品").setProductNoSnapshot("P3")
                .setContractPrice(new BigDecimal("10.000000")).setCount(new BigDecimal("2.000000"))
                .setTaxRatePercent(new BigDecimal("13.000000"));
    }
}
