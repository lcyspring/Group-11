package com.meession.etm.module.erp.service.sale;

import com.meession.etm.framework.common.exception.ServiceException;
import com.meession.etm.module.erp.api.sale.dto.ErpSaleOrderCreateReqDTO;
import com.meession.etm.module.erp.dal.dataobject.product.ErpProductDO;
import com.meession.etm.module.erp.dal.dataobject.sale.ErpCustomerDO;
import com.meession.etm.module.erp.dal.dataobject.sale.ErpSaleOrderDO;
import com.meession.etm.module.erp.dal.mysql.sale.ErpSaleOrderItemMapper;
import com.meession.etm.module.erp.dal.mysql.sale.ErpSaleOrderMapper;
import com.meession.etm.module.erp.dal.redis.no.ErpNoRedisDAO;
import com.meession.etm.module.erp.enums.ErpAuditStatus;
import com.meession.etm.module.erp.service.finance.ErpAccountService;
import com.meession.etm.module.erp.service.product.ErpProductService;
import com.meession.etm.module.system.api.user.AdminUserApi;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static com.meession.etm.module.erp.enums.ErrorCodeConstants.SALE_ORDER_EXTERNAL_IMMUTABLE;
import static com.meession.etm.module.erp.enums.ErrorCodeConstants.SALE_ORDER_EXTERNAL_SOURCE_CONFLICT;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ErpSaleOrderServiceImplTest {

    @Mock private ErpSaleOrderMapper saleOrderMapper;
    @Mock private ErpSaleOrderItemMapper saleOrderItemMapper;
    @Mock private ErpNoRedisDAO noRedisDAO;
    @Mock private ErpProductService productService;
    @Mock private ErpCustomerService customerService;
    @Mock private ErpAccountService accountService;
    @Mock private AdminUserApi adminUserApi;
    @Mock private ApplicationEventPublisher eventPublisher;
    @InjectMocks private ErpSaleOrderServiceImpl service;

    @Test
    void externalCreateReturnsExistingOrderIdempotently() {
        ErpSaleOrderDO existing = externalOrder().setId(8L).setNo("XS8").setExternalRequestHash(hash("a"));
        when(saleOrderMapper.selectByExternalSource("CRM", "CONTRACT", 7L)).thenReturn(existing);

        assertEquals(8L, service.createOrGetExternalSaleOrder(request(hash("a"))).getId());

        verify(saleOrderMapper, never()).insert(any(ErpSaleOrderDO.class));
    }

    @Test
    void externalCreateRejectsDifferentSnapshotForSameSource() {
        when(saleOrderMapper.selectByExternalSource("CRM", "CONTRACT", 7L))
                .thenReturn(externalOrder().setId(8L).setNo("XS8").setExternalRequestHash(hash("a")));

        ServiceException ex = assertThrows(ServiceException.class,
                () -> service.createOrGetExternalSaleOrder(request(hash("b"))));

        assertEquals(SALE_ORDER_EXTERNAL_SOURCE_CONFLICT.getCode(), ex.getCode());
    }

    @Test
    void externalCreatePreservesSixDecimalAmountAndSource() {
        ErpSaleOrderCreateReqDTO request = request(hash("a"));
        when(productService.validProductList(java.util.Set.of(3L))).thenReturn(List.of(
                new ErpProductDO().setId(3L).setUnitId(5L).setStatus(0)));
        when(customerService.validateCustomer(2L)).thenReturn(new ErpCustomerDO().setId(2L));
        when(noRedisDAO.generate(ErpNoRedisDAO.SALE_ORDER_NO_PREFIX)).thenReturn("XS202607160001");
        AtomicReference<ErpSaleOrderDO> inserted = new AtomicReference<>();
        doAnswer(invocation -> {
            ErpSaleOrderDO order = invocation.getArgument(0);
            order.setId(99L);
            inserted.set(order);
            return 1;
        }).when(saleOrderMapper).insert(any(ErpSaleOrderDO.class));
        when(saleOrderMapper.selectById(99L)).thenAnswer(invocation -> inserted.get());

        var result = service.createOrGetExternalSaleOrder(request);

        assertEquals(new BigDecimal("27.025831"), result.getTotalPrice());
        ArgumentCaptor<ErpSaleOrderDO> captor = ArgumentCaptor.forClass(ErpSaleOrderDO.class);
        verify(saleOrderMapper).insert(captor.capture());
        assertEquals("CRM", captor.getValue().getExternalSourceSystem());
        assertEquals("USD", captor.getValue().getSourceCurrencyCode());
        assertEquals("CNY", captor.getValue().getCurrencyCode());
    }

    @Test
    void externalOrderCannotBeEdited() {
        when(saleOrderMapper.selectById(8L)).thenReturn(externalOrder().setId(8L).setNo("XS8"));

        ServiceException ex = assertThrows(ServiceException.class,
                () -> service.updateSaleOrder(new com.meession.etm.module.erp.controller.admin.sale.vo.order.ErpSaleOrderSaveReqVO()
                        .setId(8L)));

        assertEquals(SALE_ORDER_EXTERNAL_IMMUTABLE.getCode(), ex.getCode());
    }

    private static ErpSaleOrderCreateReqDTO request(String hash) {
        return new ErpSaleOrderCreateReqDTO().setSourceSystem("CRM").setSourceType("CONTRACT").setSourceId(7L)
                .setRequestId("CRM-CONTRACT-7-V1").setRequestHash(hash).setCustomerId(2L)
                .setOrderTime(LocalDateTime.of(2026, 7, 16, 8, 0)).setSaleUserId(1L)
                .setDiscountPercent(new BigDecimal("5.500000")).setCurrencyCode("CNY")
                .setSourceCurrencyCode("USD").setExchangeRateToOrderCurrency(new BigDecimal("7.200000"))
                .setSourceGrossAmount(new BigDecimal("3.753588"))
                .setExpectedTotalPrice(new BigDecimal("27.025831")).setItems(List.of(
                        new ErpSaleOrderCreateReqDTO.Item().setProductId(3L)
                                .setProductPrice(new BigDecimal("10.123456"))
                                .setCount(new BigDecimal("2.500000"))
                                .setTaxPercent(new BigDecimal("13.000000"))));
    }

    private static ErpSaleOrderDO externalOrder() {
        return new ErpSaleOrderDO().setStatus(ErpAuditStatus.PROCESS.getStatus())
                .setExternalSourceSystem("CRM").setExternalSourceType("CONTRACT").setExternalSourceId(7L)
                .setCurrencyCode("CNY").setSourceCurrencyCode("USD")
                .setTotalPrice(BigDecimal.ONE).setTotalCount(BigDecimal.ONE)
                .setOutCount(BigDecimal.ZERO).setReturnCount(BigDecimal.ZERO);
    }

    private static String hash(String seed) {
        return seed.repeat(64);
    }
}
