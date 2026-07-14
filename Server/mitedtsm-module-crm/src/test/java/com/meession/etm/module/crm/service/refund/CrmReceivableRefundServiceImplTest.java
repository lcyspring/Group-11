package com.meession.etm.module.crm.service.refund;

import com.meession.etm.module.bpm.api.task.BpmProcessInstanceApi;
import com.meession.etm.module.bpm.enums.task.BpmProcessInstanceStatusEnum;
import com.meession.etm.module.crm.controller.admin.refund.vo.CrmReceivableRefundSaveReqVO;
import com.meession.etm.module.crm.dal.dataobject.receivable.CrmReceivableDO;
import com.meession.etm.module.crm.dal.dataobject.refund.CrmReceivableRefundActionRecordDO;
import com.meession.etm.module.crm.dal.dataobject.refund.CrmReceivableRefundDO;
import com.meession.etm.module.crm.dal.mysql.receivable.CrmReceivableMapper;
import com.meession.etm.module.crm.dal.mysql.refund.CrmReceivableRefundActionRecordMapper;
import com.meession.etm.module.crm.dal.mysql.refund.CrmReceivableRefundMapper;
import com.meession.etm.module.crm.dal.redis.no.CrmNoRedisDAO;
import com.meession.etm.module.crm.enums.common.CrmAuditStatusEnum;
import com.meession.etm.module.crm.enums.common.CrmBizTypeEnum;
import com.meession.etm.module.crm.framework.refund.CrmReceivableRefundProperties;
import com.meession.etm.module.crm.service.permission.CrmPermissionService;
import com.meession.etm.module.crm.service.permission.bo.CrmPermissionCreateReqBO;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CrmReceivableRefundServiceImplTest {

    @Mock private CrmReceivableRefundMapper refundMapper;
    @Mock private CrmReceivableRefundActionRecordMapper actionRecordMapper;
    @Mock private CrmReceivableMapper receivableMapper;
    @Mock private CrmNoRedisDAO noRedisDAO;
    @Mock private CrmPermissionService permissionService;
    @Mock private BpmProcessInstanceApi bpmProcessInstanceApi;
    @Mock private CrmReceivableRefundProperties properties;
    @InjectMocks private CrmReceivableRefundServiceImpl service;

    @Test
    void createInheritsApprovedReceivableAndCreatesOwnerPermission() {
        when(receivableMapper.selectById(8L)).thenReturn(receivable("100.00", CrmAuditStatusEnum.APPROVE));
        when(refundMapper.selectListByReceivableIdAndStatuses(eq(8L), any())).thenReturn(Collections.emptyList());
        when(properties.getNumberPrefix()).thenReturn("TK");
        when(noRedisDAO.generateMonthly("TK")).thenReturn("TK202607-0001");
        doAnswer(invocation -> { ((CrmReceivableRefundDO) invocation.getArgument(0)).setId(9L); return 1; })
                .when(refundMapper).insert(any(CrmReceivableRefundDO.class));

        assertEquals(9L, service.createRefund(request("40.00"), 1L));

        ArgumentCaptor<CrmReceivableRefundDO> refund = ArgumentCaptor.forClass(CrmReceivableRefundDO.class);
        verify(refundMapper).insert(refund.capture());
        assertEquals(30L, refund.getValue().getCustomerId());
        assertEquals(20L, refund.getValue().getContractId());
        assertEquals(40L, refund.getValue().getOwnerUserId());
        assertEquals(CrmAuditStatusEnum.DRAFT.getStatus(), refund.getValue().getAuditStatus());
        verify(permissionService).createPermission(argThat(permission ->
                permission.getBizType().equals(CrmBizTypeEnum.CRM_RECEIVABLE_REFUND.getType())
                        && permission.getBizId().equals(9L) && permission.getUserId().equals(40L)));
        verify(actionRecordMapper).insert(org.mockito.ArgumentMatchers.<CrmReceivableRefundActionRecordDO>argThat(
                action -> action.getActionType().equals(1)));
    }

    @Test
    void createRejectsUnapprovedSource() {
        when(receivableMapper.selectById(8L)).thenReturn(receivable("100.00", CrmAuditStatusEnum.PROCESS));

        assertServiceException(() -> service.createRefund(request("40.00"), 1L),
                RECEIVABLE_REFUND_SOURCE_NOT_APPROVED);
        verify(refundMapper, never()).insert(any(CrmReceivableRefundDO.class));
    }

    @Test
    void amountIncludesApprovedAndProcessingButNotDraft() {
        when(receivableMapper.selectById(8L)).thenReturn(receivable("100.00", CrmAuditStatusEnum.APPROVE));
        when(refundMapper.selectListByReceivableIdAndStatuses(eq(8L), any())).thenReturn(List.of(
                refund(1L, "30.00", CrmAuditStatusEnum.APPROVE),
                refund(2L, "20.00", CrmAuditStatusEnum.PROCESS)));

        assertServiceException(() -> service.createRefund(request("50.01"), 1L),
                RECEIVABLE_REFUND_AMOUNT_EXCEEDS, "50.00");
    }

    @Test
    void updateKeepsSourceAndOwnershipImmutableAndResetsRejectedToDraft() {
        CrmReceivableRefundDO old = refund(9L, "30.00", CrmAuditStatusEnum.REJECT)
                .setReceivableId(8L).setCustomerId(30L).setContractId(20L).setOwnerUserId(40L);
        when(refundMapper.selectByIdForUpdate(9L)).thenReturn(old);
        when(receivableMapper.selectById(8L)).thenReturn(receivable("100.00", CrmAuditStatusEnum.APPROVE));
        when(refundMapper.selectListByReceivableIdAndStatuses(eq(8L), any())).thenReturn(Collections.emptyList());
        CrmReceivableRefundSaveReqVO reqVO = request("40.00");
        reqVO.setId(9L).setReceivableId(999L);

        service.updateRefund(reqVO, 1L);

        ArgumentCaptor<CrmReceivableRefundDO> update = ArgumentCaptor.forClass(CrmReceivableRefundDO.class);
        verify(refundMapper).updateById(update.capture());
        assertNull(update.getValue().getReceivableId());
        assertNull(update.getValue().getCustomerId());
        assertNull(update.getValue().getContractId());
        assertNull(update.getValue().getOwnerUserId());
        assertEquals(CrmAuditStatusEnum.DRAFT.getStatus(), update.getValue().getAuditStatus());
    }

    @Test
    void submitLocksSourceAndReservesLatestAmount() {
        CrmReceivableRefundDO draft = refund(9L, "40.00", CrmAuditStatusEnum.DRAFT).setReceivableId(8L);
        when(refundMapper.selectById(9L)).thenReturn(draft);
        when(receivableMapper.selectByIdForUpdate(8L)).thenReturn(receivable("100.00", CrmAuditStatusEnum.APPROVE));
        when(refundMapper.selectByIdForUpdate(9L)).thenReturn(draft);
        when(refundMapper.selectListByReceivableIdAndStatuses(eq(8L), any())).thenReturn(Collections.emptyList());
        when(properties.getProcessDefinitionKey()).thenReturn("crm-receivable-refund-audit");
        when(bpmProcessInstanceApi.createProcessInstance(eq(1L), any())).thenReturn("process-1");

        service.submitRefund(9L, 1L);

        var order = inOrder(receivableMapper, refundMapper, bpmProcessInstanceApi);
        order.verify(receivableMapper).selectByIdForUpdate(8L);
        order.verify(refundMapper).selectByIdForUpdate(9L);
        order.verify(bpmProcessInstanceApi).createProcessInstance(eq(1L), any());
        verify(refundMapper).updateById(org.mockito.ArgumentMatchers.<CrmReceivableRefundDO>argThat(
                update -> update.getAuditStatus().equals(10)
                        && "process-1".equals(update.getProcessInstanceId())));
    }

    @Test
    void callbackIsIdempotentAndWritesImmutableActionOnce() {
        CrmReceivableRefundDO processing = refund(9L, "40.00", CrmAuditStatusEnum.PROCESS)
                .setProcessInstanceId("process-1");
        when(refundMapper.selectById(9L)).thenReturn(processing);
        when(refundMapper.updateAuditStatusIfProcessing(9L, "process-1", 20)).thenReturn(1);

        service.updateRefundAuditStatus(9L, "process-1", BpmProcessInstanceStatusEnum.APPROVE.getStatus());

        verify(actionRecordMapper).insert(org.mockito.ArgumentMatchers.<CrmReceivableRefundActionRecordDO>argThat(
                action -> action.getActionType().equals(4)
                        && action.getFromStatus().equals(10) && action.getToStatus().equals(20)));

        processing.setAuditStatus(20);
        service.updateRefundAuditStatus(9L, "process-1", BpmProcessInstanceStatusEnum.APPROVE.getStatus());
        verify(actionRecordMapper, times(1)).insert(any(CrmReceivableRefundActionRecordDO.class));
    }

    @Test
    void deleteRejectsPreviouslySubmittedDraft() {
        when(refundMapper.selectByIdForUpdate(9L)).thenReturn(
                refund(9L, "40.00", CrmAuditStatusEnum.DRAFT).setProcessInstanceId("old-process"));
        assertServiceException(() -> service.deleteRefund(9L, 1L), RECEIVABLE_REFUND_DELETE_STATUS_INVALID);
        verify(refundMapper, never()).deleteById(anyLong());
    }

    @Test
    void sourceSummaryShowsReservedAndRemainingAmount() {
        when(receivableMapper.selectById(8L)).thenReturn(receivable("100.00", CrmAuditStatusEnum.APPROVE));
        when(refundMapper.selectListByReceivableIdAndStatuses(eq(8L), any())).thenReturn(List.of(
                refund(1L, "30.00", CrmAuditStatusEnum.APPROVE),
                refund(2L, "20.00", CrmAuditStatusEnum.PROCESS)));

        var summary = service.getSourceSummary(8L, 2L);

        assertEquals(new BigDecimal("30.00"), summary.getReservedRefundAmount());
        assertEquals(new BigDecimal("70.00"), summary.getRemainingRefundableAmount());
    }

    private static CrmReceivableRefundSaveReqVO request(String amount) {
        return new CrmReceivableRefundSaveReqVO().setReceivableId(8L).setType(1)
                .setRefundTime(LocalDateTime.of(2026, 7, 14, 12, 0))
                .setAmount(new BigDecimal(amount)).setReason("客户重复付款，申请原路退款").setRemark("test");
    }

    private static CrmReceivableDO receivable(String amount, CrmAuditStatusEnum status) {
        return new CrmReceivableDO().setId(8L).setNo("HK-8").setCustomerId(30L).setContractId(20L)
                .setOwnerUserId(40L).setPrice(new BigDecimal(amount)).setAuditStatus(status.getStatus());
    }

    private static CrmReceivableRefundDO refund(Long id, String amount, CrmAuditStatusEnum status) {
        return new CrmReceivableRefundDO().setId(id).setAmount(new BigDecimal(amount))
                .setAuditStatus(status.getStatus());
    }
}
