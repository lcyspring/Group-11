package com.meession.etm.module.crm.service.contract;

import com.meession.etm.framework.common.exception.ServiceException;
import com.meession.etm.framework.common.util.json.JsonUtils;
import com.meession.etm.module.bpm.api.task.BpmProcessInstanceApi;
import com.meession.etm.module.crm.controller.admin.contract.vo.amendment.CrmContractAmendmentSaveReqVO;
import com.meession.etm.module.crm.dal.dataobject.contract.CrmContractAmendmentDO;
import com.meession.etm.module.crm.dal.dataobject.contract.CrmContractAttachmentDO;
import com.meession.etm.module.crm.dal.dataobject.contract.CrmContractDO;
import com.meession.etm.module.crm.dal.dataobject.contract.CrmContractProductDO;
import com.meession.etm.module.crm.dal.dataobject.contract.CrmContractSigningDO;
import com.meession.etm.module.crm.dal.mysql.contract.CrmContractAmendmentMapper;
import com.meession.etm.module.crm.dal.mysql.contract.CrmContractAttachmentMapper;
import com.meession.etm.module.crm.dal.mysql.contract.CrmContractMapper;
import com.meession.etm.module.crm.dal.mysql.contract.CrmContractProductMapper;
import com.meession.etm.module.crm.dal.mysql.contract.CrmContractSigningMapper;
import com.meession.etm.module.crm.dal.mysql.invoice.CrmInvoiceMapper;
import com.meession.etm.module.crm.dal.mysql.receivable.CrmReceivableMapper;
import com.meession.etm.module.crm.dal.mysql.receivable.CrmReceivablePlanMapper;
import com.meession.etm.module.crm.dal.redis.no.CrmNoRedisDAO;
import com.meession.etm.module.crm.enums.common.CrmAuditStatusEnum;
import com.meession.etm.module.crm.enums.contract.CrmContractLifecycleEnums;
import com.meession.etm.module.crm.framework.contract.CrmContractAmendmentProperties;
import com.meession.etm.module.crm.service.contact.CrmContactService;
import com.meession.etm.module.crm.service.product.CrmProductService;
import com.meession.etm.module.system.api.user.AdminUserApi;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.InOrder;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static com.meession.etm.module.crm.enums.ErrorCodeConstants.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CrmContractAmendmentServiceImplTest {

    @Mock private CrmContractMapper contractMapper;
    @Mock private CrmContractProductMapper productMapper;
    @Mock private CrmContractSigningMapper signingMapper;
    @Mock private CrmContractAttachmentMapper attachmentMapper;
    @Mock private CrmContractAmendmentMapper amendmentMapper;
    @Mock private CrmReceivableMapper receivableMapper;
    @Mock private CrmReceivablePlanMapper receivablePlanMapper;
    @Mock private CrmInvoiceMapper invoiceMapper;
    @Mock private CrmProductService productService;
    @Mock private CrmContactService contactService;
    @Mock private AdminUserApi adminUserApi;
    @Mock private BpmProcessInstanceApi bpmApi;
    @Mock private CrmContractLifecycleService lifecycleService;
    @Mock private CrmNoRedisDAO noRedisDAO;
    @Mock private CrmContractAmendmentProperties properties;
    @InjectMocks private CrmContractAmendmentServiceImpl service;

    @BeforeEach
    void setUp() {
        lenient().when(receivableMapper.selectReceivablePriceMapByContractId(any())).thenReturn(Map.of());
        lenient().when(receivablePlanMapper.selectListByContractId(7L)).thenReturn(List.of());
        lenient().when(invoiceMapper.selectEffectiveListByContractId(7L)).thenReturn(List.of());
    }

    @Test
    void createBuildsImmutableBeforeAfterSnapshotsAndIsIdempotent() {
        CrmContractDO contract = contract();
        CrmContractProductDO oldProduct = product();
        when(contractMapper.selectByIdForUpdate(7L)).thenReturn(contract);
        when(signingMapper.selectByContractId(7L)).thenReturn(signed());
        when(lifecycleService.getCurrentVersion(7L)).thenReturn(2);
        when(productMapper.selectListByContractId(7L)).thenReturn(List.of(oldProduct));
        when(noRedisDAO.generateMonthly("BC")).thenReturn("BC202607-0001");
        when(properties.getNumberPrefix()).thenReturn("BC");
        doAnswer(invocation -> {
            ((CrmContractAmendmentDO) invocation.getArgument(0)).setId(11L);
            return 1;
        }).when(amendmentMapper).insert(any(CrmContractAmendmentDO.class));

        Long id = service.createAmendment(request(), 1L);

        assertEquals(11L, id);
        ArgumentCaptor<CrmContractAmendmentDO> captor = ArgumentCaptor.forClass(CrmContractAmendmentDO.class);
        verify(amendmentMapper).insert(captor.capture());
        CrmContractAmendmentDO saved = captor.getValue();
        assertEquals(2, saved.getBaseVersion());
        assertEquals(3, saved.getTargetVersion());
        assertEquals(0, new BigDecimal("120.00").compareTo(saved.getAmountAfter()));
        assertEquals(0, new BigDecimal("20.00").compareTo(saved.getAmountDelta()));
        assertFalse(saved.getBeforeContractSnapshot().isBlank());
        assertFalse(saved.getAfterProductSnapshot().isBlank());
        verify(lifecycleService).recordChange(7L, CrmContractLifecycleEnums.ACTION_AMENDMENT_CREATE,
                3, 1L, "创建补充协议 BC202607-0001");

        when(amendmentMapper.selectByRequestId("req-1")).thenReturn(saved);
        assertEquals(11L, service.createAmendment(request(), 1L));
        verify(amendmentMapper, times(1)).insert(any(CrmContractAmendmentDO.class));
    }

    @Test
    void createRequiresActiveSigningFact() {
        when(contractMapper.selectByIdForUpdate(7L)).thenReturn(contract());
        when(signingMapper.selectByContractId(7L)).thenReturn(new CrmContractSigningDO()
                .setStatus(CrmContractLifecycleEnums.SIGN_VOIDED));

        ServiceException ex = assertThrows(ServiceException.class,
                () -> service.createAmendment(request(), 1L));

        assertEquals(CONTRACT_AMENDMENT_REQUIRES_SIGNED.getCode(), ex.getCode());
        verify(amendmentMapper, never()).insert(any(CrmContractAmendmentDO.class));
    }

    @Test
    void createRejectsAmountBelowExistingFinancialCommitments() {
        CrmContractAmendmentSaveReqVO req = request();
        req.getProducts().get(0).setContractPrice(new BigDecimal("80"));
        when(contractMapper.selectByIdForUpdate(7L)).thenReturn(contract());
        when(signingMapper.selectByContractId(7L)).thenReturn(signed());
        when(lifecycleService.getCurrentVersion(7L)).thenReturn(2);
        when(productMapper.selectListByContractId(7L)).thenReturn(List.of(product()));
        when(receivableMapper.selectReceivablePriceMapByContractId(any())).thenReturn(Map.of(7L, new BigDecimal("90")));

        ServiceException ex = assertThrows(ServiceException.class,
                () -> service.createAmendment(req, 1L));

        assertEquals(CONTRACT_AMENDMENT_FINANCIAL_FLOOR.getCode(), ex.getCode());
    }

    @Test
    void createRejectsDuplicateContractProductRows() {
        CrmContractAmendmentSaveReqVO req = request();
        req.setProducts(List.of(req.getProducts().get(0), req.getProducts().get(0)));
        when(contractMapper.selectByIdForUpdate(7L)).thenReturn(contract());
        when(signingMapper.selectByContractId(7L)).thenReturn(signed());
        when(lifecycleService.getCurrentVersion(7L)).thenReturn(2);
        when(productMapper.selectListByContractId(7L)).thenReturn(List.of(product()));

        ServiceException ex = assertThrows(ServiceException.class,
                () -> service.createAmendment(req, 1L));

        assertEquals(CONTRACT_PRODUCT_ROW_DUPLICATE.getCode(), ex.getCode());
        verify(amendmentMapper, never()).insert(any(CrmContractAmendmentDO.class));
    }

    @Test
    void createPreservesExistingTaxRateAndRecalculatesTaxAfterDiscount() {
        CrmContractAmendmentSaveReqVO req = request().setDiscountPercent(new BigDecimal("10"));
        CrmContractProductDO oldProduct = product().setTaxRatePercent(new BigDecimal("13"));
        when(contractMapper.selectByIdForUpdate(7L)).thenReturn(contract());
        when(signingMapper.selectByContractId(7L)).thenReturn(signed());
        when(lifecycleService.getCurrentVersion(7L)).thenReturn(2);
        when(productMapper.selectListByContractId(7L)).thenReturn(List.of(oldProduct));
        when(noRedisDAO.generateMonthly("BC")).thenReturn("BC202607-0001");
        when(properties.getNumberPrefix()).thenReturn("BC");

        service.createAmendment(req, 1L);

        ArgumentCaptor<CrmContractAmendmentDO> captor = ArgumentCaptor.forClass(CrmContractAmendmentDO.class);
        verify(amendmentMapper).insert(captor.capture());
        CrmContractDO after = JsonUtils.parseObject(captor.getValue().getAfterContractSnapshot(), CrmContractDO.class);
        List<CrmContractProductDO> afterProducts = JsonUtils.parseArray(
                captor.getValue().getAfterProductSnapshot(), CrmContractProductDO.class);
        assertEquals(0, new BigDecimal("108.00").compareTo(after.getTotalPrice()));
        assertEquals(0, new BigDecimal("14.04").compareTo(after.getTaxAmount()));
        assertEquals(0, new BigDecimal("122.04").compareTo(after.getGrossAmount()));
        assertEquals(0, new BigDecimal("13").compareTo(afterProducts.get(0).getTaxRatePercent()));
        assertEquals(0, new BigDecimal("14.04").compareTo(afterProducts.get(0).getTaxAmount()));
    }

    @Test
    void submitRequiresEvidenceThenStartsDedicatedProcess() {
        CrmContractAmendmentDO amendment = amendment().setAuditStatus(CrmAuditStatusEnum.DRAFT.getStatus());
        when(amendmentMapper.selectByIdForUpdate(11L)).thenReturn(amendment);
        when(contractMapper.selectByIdForUpdate(7L)).thenReturn(contract());
        when(signingMapper.selectByContractId(7L)).thenReturn(signed());
        when(lifecycleService.getCurrentVersion(7L)).thenReturn(2);

        ServiceException ex = assertThrows(ServiceException.class,
                () -> service.submitAmendment(7L, 11L, 1L));
        assertEquals(CONTRACT_AMENDMENT_EVIDENCE_REQUIRED.getCode(), ex.getCode());

        when(attachmentMapper.selectListByAmendmentId(11L)).thenReturn(List.of(
                new CrmContractAttachmentDO().setId(21L).setAmendmentId(11L)));
        when(properties.getProcessDefinitionKey()).thenReturn("crm-contract-amendment-audit");
        when(bpmApi.createProcessInstance(eq(1L), any())).thenReturn("process-1");

        service.submitAmendment(7L, 11L, 1L);

        verify(amendmentMapper).updateById(ArgumentMatchers.<CrmContractAmendmentDO>argThat(item -> item.getId().equals(11L)
                && item.getAuditStatus().equals(CrmAuditStatusEnum.PROCESS.getStatus())
                && "process-1".equals(item.getProcessInstanceId())));
    }

    @Test
    void submitRejectsAmendmentFromAnotherContract() {
        when(amendmentMapper.selectByIdForUpdate(11L)).thenReturn(amendment().setContractId(8L));

        ServiceException ex = assertThrows(ServiceException.class,
                () -> service.submitAmendment(7L, 11L, 1L));

        assertEquals(CONTRACT_AMENDMENT_NOT_BELONGS.getCode(), ex.getCode());
        verifyNoInteractions(bpmApi);
    }

    @Test
    void approvalAppliesProjectionLocksEvidenceAndAdvancesVersion() {
        CrmContractDO after = contract().setName("合同 V3").setTotalProductPrice(new BigDecimal("120"))
                .setTotalPrice(new BigDecimal("120"));
        CrmContractProductDO afterProduct = product().setId(null).setContractPrice(new BigDecimal("120"))
                .setTotalPrice(new BigDecimal("120"));
        CrmContractAmendmentDO amendment = amendment().setAuditStatus(CrmAuditStatusEnum.PROCESS.getStatus())
                .setProcessInstanceId("process-1").setAfterContractSnapshot(JsonUtils.toJsonString(after))
                .setAfterProductSnapshot(JsonUtils.toJsonString(List.of(afterProduct)));
        when(amendmentMapper.selectByIdForUpdate(11L)).thenReturn(amendment);
        when(contractMapper.selectByIdForUpdate(7L)).thenReturn(contract());
        when(lifecycleService.getCurrentVersion(7L)).thenReturn(2);
        when(productMapper.selectListByContractId(7L)).thenReturn(List.of(product()));
        when(attachmentMapper.selectListByAmendmentId(11L)).thenReturn(List.of(
                new CrmContractAttachmentDO().setId(21L).setAmendmentId(11L)));
        when(amendmentMapper.updateAuditStatusIfProcessing(eq(11L), eq("process-1"),
                eq(CrmAuditStatusEnum.APPROVE.getStatus()), any())).thenReturn(1);

        service.updateAuditStatus(11L, "process-1", 2);

        InOrder stateBeforeProjection = inOrder(amendmentMapper, contractMapper);
        stateBeforeProjection.verify(amendmentMapper).selectByIdForUpdate(11L);
        stateBeforeProjection.verify(amendmentMapper).updateAuditStatusIfProcessing(eq(11L), eq("process-1"),
                eq(CrmAuditStatusEnum.APPROVE.getStatus()), any());
        stateBeforeProjection.verify(contractMapper).selectByIdForUpdate(7L);
        verify(contractMapper).updateById(ArgumentMatchers.<CrmContractDO>argThat(item -> "合同 V3".equals(item.getName())
                && new BigDecimal("120").compareTo(item.getTotalPrice()) == 0));
        verify(productMapper).deleteByIds(List.of(31L));
        verify(productMapper).insertBatch(ArgumentMatchers.<Collection<CrmContractProductDO>>argThat(
                items -> items.size() == 1 && items.iterator().next().getId() == null
                        && items.iterator().next().getContractId().equals(7L)));
        verify(attachmentMapper).updateById(ArgumentMatchers.<CrmContractAttachmentDO>argThat(item -> item.getId().equals(21L)
                && Boolean.TRUE.equals(item.getImmutable())));
        verify(lifecycleService).recordChange(eq(7L),
                eq(CrmContractLifecycleEnums.ACTION_AMENDMENT_EFFECTIVE), eq(3), isNull(), contains("审批状态"));
    }

    @Test
    void staleAndDuplicateCallbacksDoNotRewriteContractProjection() {
        CrmContractAmendmentDO processing = amendment().setAuditStatus(CrmAuditStatusEnum.PROCESS.getStatus())
                .setProcessInstanceId("process-current");
        when(amendmentMapper.selectByIdForUpdate(11L)).thenReturn(processing);

        service.updateAuditStatus(11L, "process-old", 2);

        verify(amendmentMapper, never()).updateAuditStatusIfProcessing(any(), any(), any(), any());
        verify(contractMapper, never()).updateById(any(CrmContractDO.class));

        reset(amendmentMapper, contractMapper);
        when(amendmentMapper.selectByIdForUpdate(11L)).thenReturn(
                amendment().setAuditStatus(CrmAuditStatusEnum.APPROVE.getStatus())
                        .setProcessInstanceId("process-current"));

        service.updateAuditStatus(11L, "process-current", 2);

        verify(amendmentMapper, never()).updateAuditStatusIfProcessing(any(), any(), any(), any());
        verify(contractMapper, never()).updateById(any(CrmContractDO.class));
    }

    @Test
    void lostConditionalTerminalUpdateDoesNotApplyProjection() {
        when(amendmentMapper.selectByIdForUpdate(11L)).thenReturn(
                amendment().setAuditStatus(CrmAuditStatusEnum.PROCESS.getStatus())
                        .setProcessInstanceId("process-1"));
        when(amendmentMapper.updateAuditStatusIfProcessing(eq(11L), eq("process-1"),
                eq(CrmAuditStatusEnum.APPROVE.getStatus()), any())).thenReturn(0);

        service.updateAuditStatus(11L, "process-1", 2);

        verify(contractMapper, never()).selectByIdForUpdate(any());
        verify(contractMapper, never()).updateById(any(CrmContractDO.class));
        verify(lifecycleService, never()).recordChange(any(), any(), any(), any(), any());
    }

    @Test
    void rejectionOnlyClosesApprovalWithoutChangingProjection() {
        when(amendmentMapper.selectByIdForUpdate(11L)).thenReturn(
                amendment().setAuditStatus(CrmAuditStatusEnum.PROCESS.getStatus())
                        .setProcessInstanceId("process-1"));
        when(amendmentMapper.updateAuditStatusIfProcessing(eq(11L), eq("process-1"),
                eq(CrmAuditStatusEnum.REJECT.getStatus()), isNull())).thenReturn(1);

        service.updateAuditStatus(11L, "process-1", 3);

        verify(contractMapper, never()).updateById(any(CrmContractDO.class));
        verify(lifecycleService).recordChange(eq(7L), eq(CrmContractLifecycleEnums.ACTION_AMENDMENT_REJECT),
                eq(3), isNull(), contains("审批状态"));
    }

    private static CrmContractAmendmentSaveReqVO request() {
        CrmContractAmendmentSaveReqVO req = new CrmContractAmendmentSaveReqVO().setContractId(7L)
                .setClientRequestId("req-1").setTitle("价格补充协议").setReason("范围增加")
                .setContractName("合同 V3").setDiscountPercent(BigDecimal.ZERO);
        req.setProducts(List.of(new CrmContractAmendmentSaveReqVO.Product().setId(31L).setProductId(41L)
                .setContractPrice(new BigDecimal("120")).setCount(BigDecimal.ONE)));
        return req;
    }

    private static CrmContractDO contract() {
        return new CrmContractDO().setId(7L).setName("合同").setCustomerId(8L)
                .setAuditStatus(CrmAuditStatusEnum.APPROVE.getStatus()).setTotalProductPrice(new BigDecimal("100.00"))
                .setTotalPrice(new BigDecimal("100.00")).setDiscountPercent(BigDecimal.ZERO)
                .setExchangeRateToBase(BigDecimal.ONE);
    }

    private static CrmContractProductDO product() {
        return new CrmContractProductDO().setId(31L).setContractId(7L).setProductId(41L)
                .setProductNameSnapshot("产品").setProductNoSnapshot("P1").setProductUnitSnapshot(1)
                .setProductVersionSnapshot(1).setProductPrice(new BigDecimal("100"))
                .setContractPrice(new BigDecimal("100")).setCount(BigDecimal.ONE)
                .setTotalPrice(new BigDecimal("100"));
    }

    private static CrmContractSigningDO signed() {
        return new CrmContractSigningDO().setContractId(7L).setStatus(CrmContractLifecycleEnums.SIGNED);
    }

    private static CrmContractAmendmentDO amendment() {
        return new CrmContractAmendmentDO().setId(11L).setContractId(7L).setNo("BC202607-0001")
                .setBaseVersion(2).setTargetVersion(3).setAmountAfter(new BigDecimal("120"));
    }
}
