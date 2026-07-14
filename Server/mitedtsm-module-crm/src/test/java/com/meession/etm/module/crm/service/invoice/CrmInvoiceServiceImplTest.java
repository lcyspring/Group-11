package com.meession.etm.module.crm.service.invoice;

import com.meession.etm.framework.common.exception.ServiceException;
import com.meession.etm.module.crm.controller.admin.invoice.vo.*;
import com.meession.etm.module.crm.dal.dataobject.contract.CrmContractDO;
import com.meession.etm.module.crm.dal.dataobject.invoice.CrmInvoiceActionRecordDO;
import com.meession.etm.module.crm.dal.dataobject.invoice.CrmInvoiceDO;
import com.meession.etm.module.crm.dal.mysql.invoice.CrmInvoiceActionRecordMapper;
import com.meession.etm.module.crm.dal.mysql.invoice.CrmInvoiceMapper;
import com.meession.etm.module.crm.dal.redis.no.CrmNoRedisDAO;
import com.meession.etm.module.crm.enums.common.CrmAuditStatusEnum;
import com.meession.etm.module.crm.enums.common.CrmBizTypeEnum;
import com.meession.etm.module.crm.enums.invoice.CrmInvoiceDirectionEnum;
import com.meession.etm.module.crm.enums.invoice.CrmInvoiceStatusEnum;
import com.meession.etm.module.crm.enums.invoice.CrmInvoiceTypeEnum;
import com.meession.etm.module.crm.enums.permission.CrmPermissionLevelEnum;
import com.meession.etm.module.crm.framework.invoice.CrmInvoiceProvider;
import com.meession.etm.module.crm.framework.permission.core.annotations.CrmPermission;
import com.meession.etm.module.crm.service.contract.CrmContractService;
import com.meession.etm.module.crm.service.permission.CrmPermissionService;
import com.meession.etm.module.crm.service.permission.bo.CrmPermissionCreateReqBO;
import com.meession.etm.module.system.api.user.AdminUserApi;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import static com.meession.etm.framework.test.core.util.AssertUtils.assertServiceException;
import static com.meession.etm.module.crm.enums.ErrorCodeConstants.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CrmInvoiceServiceImplTest {

    @Mock private CrmInvoiceMapper invoiceMapper;
    @Mock private CrmInvoiceActionRecordMapper actionRecordMapper;
    @Mock private CrmNoRedisDAO noRedisDAO;
    @Mock private CrmContractService contractService;
    @Mock private CrmPermissionService permissionService;
    @Mock private AdminUserApi adminUserApi;
    @Mock private CrmInvoiceProvider invoiceProvider;
    @InjectMocks private CrmInvoiceServiceImpl service;

    @Test
    void createInheritsContractCustomerAndOwnerAndWritesAuditTrail() {
        when(contractService.validateContract(20L)).thenReturn(contract("100.00"));
        when(invoiceMapper.selectEffectiveListByContractId(20L)).thenReturn(Collections.emptyList());
        when(noRedisDAO.generateMonthly(CrmNoRedisDAO.INVOICE_PREFIX)).thenReturn("FP2026070001");
        doAnswer(invocation -> { ((CrmInvoiceDO) invocation.getArgument(0)).setId(10L); return 1; })
                .when(invoiceMapper).insert(any(CrmInvoiceDO.class));

        assertEquals(10L, service.createInvoice(createRequest("40.00"), 1L));

        ArgumentCaptor<CrmInvoiceDO> invoice = ArgumentCaptor.forClass(CrmInvoiceDO.class);
        verify(invoiceMapper).insert(invoice.capture());
        assertEquals(30L, invoice.getValue().getCustomerId());
        assertEquals(40L, invoice.getValue().getOwnerUserId());
        assertEquals(CrmInvoiceDirectionEnum.BLUE.getDirection(), invoice.getValue().getDirection());
        assertEquals(CrmInvoiceStatusEnum.DRAFT.getStatus(), invoice.getValue().getStatus());
        ArgumentCaptor<CrmPermissionCreateReqBO> permission = ArgumentCaptor.forClass(CrmPermissionCreateReqBO.class);
        verify(permissionService).createPermission(permission.capture());
        assertEquals(CrmBizTypeEnum.CRM_INVOICE.getType(), permission.getValue().getBizType());
        assertEquals(40L, permission.getValue().getUserId());
        verify(actionRecordMapper).insert(org.mockito.ArgumentMatchers.<CrmInvoiceActionRecordDO>argThat(
                record -> record.getInvoiceId().equals(10L)
                && record.getActionType().equals(1) && record.getToStatus().equals(0)));
    }

    @Test
    void createRejectsUnapprovedContract() {
        when(contractService.validateContract(20L)).thenReturn(contract("100.00")
                .setAuditStatus(CrmAuditStatusEnum.PROCESS.getStatus()));

        assertServiceException(() -> service.createInvoice(createRequest("40.00"), 1L),
                INVOICE_CONTRACT_NOT_APPROVED);
        verify(invoiceMapper, never()).insert(any(CrmInvoiceDO.class));
    }

    @Test
    void specialInvoiceRequiresCompleteBuyerTaxSnapshot() {
        when(contractService.validateContract(20L)).thenReturn(contract("100.00"));
        CrmInvoiceCreateReqVO request = createRequest("40.00");
        request.setType(CrmInvoiceTypeEnum.VAT_SPECIAL.getType());

        assertServiceException(() -> service.createInvoice(request, 1L), INVOICE_SPECIAL_BUYER_INFO_REQUIRED);
        verify(invoiceMapper, never()).insert(any(CrmInvoiceDO.class));
    }

    @Test
    void updateUsesExplicitDraftFieldWhitelist() {
        when(invoiceMapper.selectByIdForUpdate(10L)).thenReturn(blue(10L, CrmInvoiceStatusEnum.DRAFT, "40.00"));
        when(contractService.validateContract(20L)).thenReturn(contract("100.00"));
        when(invoiceMapper.selectEffectiveListByContractId(20L)).thenReturn(Collections.emptyList());

        service.updateInvoice(updateRequest("55.00"), 1L);

        ArgumentCaptor<CrmInvoiceDO> update = ArgumentCaptor.forClass(CrmInvoiceDO.class);
        verify(invoiceMapper).updateById(update.capture());
        assertEquals(10L, update.getValue().getId());
        assertNull(update.getValue().getContractId());
        assertNull(update.getValue().getCustomerId());
        assertNull(update.getValue().getOwnerUserId());
        assertNull(update.getValue().getStatus());
        assertNull(update.getValue().getDirection());
        assertEquals(new BigDecimal("55.00"), update.getValue().getAmount());
    }

    @Test
    void formalInvoiceCannotBeUpdatedOrDeleted() {
        CrmInvoiceDO issued = blue(10L, CrmInvoiceStatusEnum.ISSUED, "40.00");
        when(invoiceMapper.selectByIdForUpdate(10L)).thenReturn(issued);
        assertServiceException(() -> service.updateInvoice(updateRequest("55.00"), 1L), INVOICE_DRAFT_ONLY);
        assertServiceException(() -> service.deleteInvoice(10L, 1L), INVOICE_DRAFT_ONLY);
        verify(invoiceMapper, never()).updateById(any(CrmInvoiceDO.class));
        verify(invoiceMapper, never()).deleteById(anyLong());
    }

    @Test
    void issueLocksContractBeforeInvoiceAndRejectsExceededNetAmount() {
        CrmInvoiceDO draft = blue(10L, CrmInvoiceStatusEnum.DRAFT, "40.00");
        when(invoiceMapper.selectById(10L)).thenReturn(draft);
        when(contractService.validateContractForUpdate(20L)).thenReturn(contract("100.00"));
        when(invoiceMapper.selectByIdForUpdate(10L)).thenReturn(draft);
        when(invoiceMapper.selectEffectiveListByContractId(20L)).thenReturn(List.of(
                blue(11L, CrmInvoiceStatusEnum.ISSUED, "80.00")));

        ServiceException error = assertThrows(ServiceException.class,
                () -> service.issueInvoice(issueRequest(), 1L));
        assertEquals(INVOICE_AMOUNT_EXCEEDS_CONTRACT.getCode(), error.getCode());
        assertEquals("开票金额超出合同剩余可开票金额，目前可开票：20.00 元", error.getMessage());
        var order = inOrder(contractService, invoiceMapper);
        order.verify(contractService).validateContractForUpdate(20L);
        order.verify(invoiceMapper).selectByIdForUpdate(10L);
        verify(invoiceProvider, never()).issue(any(), any());
    }

    @Test
    void issueUsesStableIdempotencyKeyAndPersistsProviderIdentity() {
        CrmInvoiceDO draft = blue(10L, CrmInvoiceStatusEnum.DRAFT, "40.00");
        when(invoiceMapper.selectById(10L)).thenReturn(draft);
        when(contractService.validateContractForUpdate(20L)).thenReturn(contract("100.00"));
        when(invoiceMapper.selectByIdForUpdate(10L)).thenReturn(draft);
        when(invoiceMapper.selectEffectiveListByContractId(20L)).thenReturn(Collections.emptyList());
        when(invoiceProvider.issue(any(), eq("invoice:issue:10")))
                .thenReturn(new CrmInvoiceProvider.ProviderResult("local-ledger", "invoice:issue:10", null));

        service.issueInvoice(issueRequest(), 1L);

        verify(invoiceProvider).issue(any(), eq("invoice:issue:10"));
        verify(invoiceMapper).updateById(org.mockito.ArgumentMatchers.<CrmInvoiceDO>argThat(
                update -> update.getStatus().equals(10)
                && update.getExternalProvider().equals("local-ledger")
                && update.getExternalRequestId().equals("invoice:issue:10")));
        verify(actionRecordMapper).insert(org.mockito.ArgumentMatchers.<CrmInvoiceActionRecordDO>argThat(
                record -> "invoice:issue:10".equals(record.getProviderRequestId())));
    }

    @Test
    void providerCannotAcknowledgeACommandWithAnotherIdempotencyKey() {
        CrmInvoiceDO draft = blue(10L, CrmInvoiceStatusEnum.DRAFT, "40.00");
        when(invoiceMapper.selectById(10L)).thenReturn(draft);
        when(contractService.validateContractForUpdate(20L)).thenReturn(contract("100.00"));
        when(invoiceMapper.selectByIdForUpdate(10L)).thenReturn(draft);
        when(invoiceMapper.selectEffectiveListByContractId(20L)).thenReturn(Collections.emptyList());
        when(invoiceProvider.issue(any(), eq("invoice:issue:10")))
                .thenReturn(new CrmInvoiceProvider.ProviderResult("external", "wrong-request", "E-1"));

        assertServiceException(() -> service.issueInvoice(issueRequest(), 1L), INVOICE_PROVIDER_RESULT_INVALID);
        verify(invoiceMapper, never()).updateById(any(CrmInvoiceDO.class));
        verify(actionRecordMapper, never()).insert(any(CrmInvoiceActionRecordDO.class));
    }

    @Test
    void partialRedFlushCreatesIndependentRedInvoiceWithStableBusinessKey() {
        CrmInvoiceDO original = blue(10L, CrmInvoiceStatusEnum.ISSUED, "100.00").setRedAmount(new BigDecimal("20.00"));
        when(invoiceMapper.selectById(10L)).thenReturn(original);
        when(invoiceMapper.selectByIdForUpdate(10L)).thenReturn(original);
        when(noRedisDAO.generateMonthly(CrmNoRedisDAO.INVOICE_PREFIX)).thenReturn("FP2026070002");
        doAnswer(invocation -> { ((CrmInvoiceDO) invocation.getArgument(0)).setId(12L); return 1; })
                .when(invoiceMapper).insert(any(CrmInvoiceDO.class));
        when(invoiceProvider.redFlush(eq(original), any(), eq("invoice:red:10:R-001")))
                .thenReturn(new CrmInvoiceProvider.ProviderResult("local-ledger", "invoice:red:10:R-001", null));

        assertEquals(12L, service.redFlushInvoice(redRequest("30.00"), 1L));

        verify(invoiceProvider).redFlush(eq(original), any(), eq("invoice:red:10:R-001"));
        verify(invoiceMapper).updateById(org.mockito.ArgumentMatchers.<CrmInvoiceDO>argThat(
                update -> update.getId().equals(10L)
                && new BigDecimal("50.00").equals(update.getRedAmount())
                && update.getStatus().equals(CrmInvoiceStatusEnum.PARTIALLY_RED.getStatus())));
        verify(permissionService).createPermission(argThat(permission -> permission.getBizId().equals(12L)));
    }

    @Test
    void completeRedFlushMovesOriginalToFullyRed() {
        CrmInvoiceDO original = blue(10L, CrmInvoiceStatusEnum.PARTIALLY_RED, "100.00")
                .setRedAmount(new BigDecimal("70.00"));
        stubRedFlush(original);

        service.redFlushInvoice(redRequest("30.00"), 1L);

        verify(invoiceMapper).updateById(org.mockito.ArgumentMatchers.<CrmInvoiceDO>argThat(
                update -> update.getId().equals(10L)
                && update.getStatus().equals(CrmInvoiceStatusEnum.FULLY_RED.getStatus())));
    }

    @Test
    void redFlushCannotExceedRemainingAmountOrTargetAnotherRedInvoice() {
        CrmInvoiceDO original = blue(10L, CrmInvoiceStatusEnum.PARTIALLY_RED, "100.00")
                .setRedAmount(new BigDecimal("90.00"));
        when(invoiceMapper.selectById(10L)).thenReturn(original);
        when(invoiceMapper.selectByIdForUpdate(10L)).thenReturn(original);
        ServiceException error = assertThrows(ServiceException.class,
                () -> service.redFlushInvoice(redRequest("20.00"), 1L));
        assertEquals(INVOICE_RED_AMOUNT_EXCEEDS.getCode(), error.getCode());
        assertEquals("红冲金额超出原发票剩余可红冲金额，目前可红冲：10.00 元", error.getMessage());

        CrmInvoiceDO red = original.setDirection(CrmInvoiceDirectionEnum.RED.getDirection()).setOriginalInvoiceId(9L);
        when(invoiceMapper.selectById(10L)).thenReturn(red);
        when(invoiceMapper.selectByIdForUpdate(10L)).thenReturn(red);
        assertServiceException(() -> service.redFlushInvoice(redRequest("1.00"), 1L), INVOICE_RED_CANNOT_RED);
    }

    @Test
    void blueInvoiceWithActiveCreditCannotBeVoided() {
        CrmInvoiceDO original = blue(10L, CrmInvoiceStatusEnum.ISSUED, "100.00");
        when(invoiceMapper.selectById(10L)).thenReturn(original);
        when(invoiceMapper.selectByIdForUpdate(10L)).thenReturn(original);
        when(invoiceMapper.selectActiveRedList(10L)).thenReturn(List.of(red(12L, 10L, "20.00")));

        assertServiceException(() -> service.voidInvoice(voidRequest(10L), 1L), INVOICE_VOID_HAS_RED);
        verify(invoiceProvider, never()).voidInvoice(any(), any());
    }

    @Test
    void voidingRedInvoiceRestoresOriginalAmountAndStatus() {
        CrmInvoiceDO red = red(12L, 10L, "20.00");
        CrmInvoiceDO original = blue(10L, CrmInvoiceStatusEnum.PARTIALLY_RED, "100.00")
                .setRedAmount(new BigDecimal("20.00"));
        when(invoiceMapper.selectById(12L)).thenReturn(red);
        when(invoiceMapper.selectByIdForUpdate(12L)).thenReturn(red);
        when(invoiceMapper.selectByIdForUpdate(10L)).thenReturn(original);
        when(invoiceProvider.voidInvoice(red, "invoice:void:12"))
                .thenReturn(new CrmInvoiceProvider.ProviderResult("local-ledger", "invoice:void:12", null));

        service.voidInvoice(voidRequest(12L), 1L);

        verify(invoiceMapper).updateById(org.mockito.ArgumentMatchers.<CrmInvoiceDO>argThat(
                update -> update.getId().equals(10L)
                && update.getRedAmount().compareTo(BigDecimal.ZERO) == 0
                && update.getStatus().equals(CrmInvoiceStatusEnum.ISSUED.getStatus())));
        verify(actionRecordMapper, times(2)).insert(any(CrmInvoiceActionRecordDO.class));
    }

    @Test
    void contractSummaryUsesEffectiveBlueMinusRedLedger() {
        when(contractService.validateContract(20L)).thenReturn(contract("200.00"));
        when(invoiceMapper.selectEffectiveListByContractId(20L)).thenReturn(List.of(
                blue(10L, CrmInvoiceStatusEnum.PARTIALLY_RED, "100.00"),
                blue(11L, CrmInvoiceStatusEnum.ISSUED, "50.00"), red(12L, 10L, "30.00")));

        CrmInvoiceSummaryRespVO summary = service.getContractSummary(20L);

        assertEquals(new BigDecimal("150.00"), summary.getBlueAmount());
        assertEquals(new BigDecimal("30.00"), summary.getRedAmount());
        assertEquals(new BigDecimal("120.00"), summary.getNetAmount());
        assertEquals(new BigDecimal("80.00"), summary.getAvailableAmount());
    }

    @Test
    void commandsDeclareObjectPermissionBoundaries() throws NoSuchMethodException {
        CrmPermission create = CrmInvoiceServiceImpl.class
                .getMethod("createInvoice", CrmInvoiceCreateReqVO.class, Long.class).getAnnotation(CrmPermission.class);
        assertEquals(CrmBizTypeEnum.CRM_CONTRACT, create.bizType()[0]);
        assertEquals(CrmPermissionLevelEnum.WRITE, create.level());
        CrmPermission issue = CrmInvoiceServiceImpl.class
                .getMethod("issueInvoice", CrmInvoiceIssueReqVO.class, Long.class).getAnnotation(CrmPermission.class);
        assertEquals(CrmBizTypeEnum.CRM_INVOICE, issue.bizType()[0]);
        assertEquals(CrmPermissionLevelEnum.WRITE, issue.level());
    }

    private void stubRedFlush(CrmInvoiceDO original) {
        when(invoiceMapper.selectById(10L)).thenReturn(original);
        when(invoiceMapper.selectByIdForUpdate(10L)).thenReturn(original);
        when(noRedisDAO.generateMonthly(CrmNoRedisDAO.INVOICE_PREFIX)).thenReturn("FP2026070002");
        doAnswer(invocation -> { ((CrmInvoiceDO) invocation.getArgument(0)).setId(12L); return 1; })
                .when(invoiceMapper).insert(any(CrmInvoiceDO.class));
        when(invoiceProvider.redFlush(any(), any(), any()))
                .thenReturn(new CrmInvoiceProvider.ProviderResult("local-ledger", "invoice:red:10:R-001", null));
    }

    private static CrmContractDO contract(String amount) {
        return new CrmContractDO().setId(20L).setCustomerId(30L).setOwnerUserId(40L)
                .setAuditStatus(CrmAuditStatusEnum.APPROVE.getStatus()).setTotalPrice(new BigDecimal(amount));
    }

    private static CrmInvoiceCreateReqVO createRequest(String amount) {
        CrmInvoiceCreateReqVO request = new CrmInvoiceCreateReqVO();
        request.setContractId(20L);
        request.setHandlerUserId(50L).setType(CrmInvoiceTypeEnum.VAT_ORDINARY.getType())
                .setAmount(new BigDecimal(amount)).setTitle("测试客户")
                .setContent("技术服务费").setRemark("草稿");
        return request;
    }

    private static CrmInvoiceUpdateReqVO updateRequest(String amount) {
        CrmInvoiceUpdateReqVO request = new CrmInvoiceUpdateReqVO();
        request.setId(10L);
        request.setHandlerUserId(50L).setType(CrmInvoiceTypeEnum.VAT_ORDINARY.getType())
                .setAmount(new BigDecimal(amount)).setTitle("修改客户")
                .setContent("修改服务费").setRemark("修改");
        return request;
    }

    private static CrmInvoiceIssueReqVO issueRequest() {
        return new CrmInvoiceIssueReqVO().setId(10L).setInvoiceNo("B-001")
                .setInvoiceDate(LocalDateTime.of(2026, 7, 14, 10, 0)).setHandlerUserId(50L);
    }

    private static CrmInvoiceRedFlushReqVO redRequest(String amount) {
        return new CrmInvoiceRedFlushReqVO().setOriginalInvoiceId(10L).setAmount(new BigDecimal(amount))
                .setInvoiceNo("R-001").setInvoiceDate(LocalDateTime.of(2026, 7, 14, 11, 0))
                .setHandlerUserId(50L).setReason("销售退回");
    }

    private static CrmInvoiceVoidReqVO voidRequest(Long id) {
        return new CrmInvoiceVoidReqVO().setId(id).setReason("票面信息错误");
    }

    private static CrmInvoiceDO blue(Long id, CrmInvoiceStatusEnum status, String amount) {
        return new CrmInvoiceDO().setId(id).setNo("FP-" + id).setContractId(20L).setCustomerId(30L)
                .setOwnerUserId(40L).setHandlerUserId(50L).setDirection(CrmInvoiceDirectionEnum.BLUE.getDirection())
                .setStatus(status.getStatus()).setType(CrmInvoiceTypeEnum.VAT_ORDINARY.getType())
                .setAmount(new BigDecimal(amount)).setRedAmount(BigDecimal.ZERO).setTitle("测试客户").setContent("服务费");
    }

    private static CrmInvoiceDO red(Long id, Long originalId, String amount) {
        return blue(id, CrmInvoiceStatusEnum.ISSUED, amount)
                .setDirection(CrmInvoiceDirectionEnum.RED.getDirection()).setOriginalInvoiceId(originalId)
                .setInvoiceNo("R-" + id).setInvoiceDate(LocalDateTime.of(2026, 7, 14, 11, 0));
    }
}
