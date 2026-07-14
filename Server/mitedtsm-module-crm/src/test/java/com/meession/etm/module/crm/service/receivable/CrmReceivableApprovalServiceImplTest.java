package com.meession.etm.module.crm.service.receivable;

import com.meession.etm.framework.common.exception.ServiceException;
import com.meession.etm.module.bpm.api.task.BpmProcessInstanceApi;
import com.meession.etm.module.bpm.api.task.dto.BpmProcessInstanceCreateReqDTO;
import com.meession.etm.module.bpm.enums.task.BpmProcessInstanceStatusEnum;
import com.meession.etm.module.crm.controller.admin.receivable.vo.receivable.CrmReceivableSaveReqVO;
import com.meession.etm.module.crm.dal.dataobject.contract.CrmContractDO;
import com.meession.etm.module.crm.dal.dataobject.receivable.CrmReceivableDO;
import com.meession.etm.module.crm.dal.mysql.receivable.CrmReceivableMapper;
import com.meession.etm.module.crm.dal.redis.no.CrmNoRedisDAO;
import com.meession.etm.module.crm.enums.common.CrmAuditStatusEnum;
import com.meession.etm.module.crm.enums.common.CrmBizTypeEnum;
import com.meession.etm.module.crm.enums.permission.CrmPermissionLevelEnum;
import com.meession.etm.module.crm.framework.permission.core.annotations.CrmPermission;
import com.meession.etm.module.crm.service.contract.CrmContractService;
import com.meession.etm.module.crm.service.permission.CrmPermissionService;
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

import static com.meession.etm.module.crm.enums.ErrorCodeConstants.RECEIVABLE_DELETE_FAIL_NOT_NEW_DRAFT;
import static com.meession.etm.module.crm.enums.ErrorCodeConstants.RECEIVABLE_UPDATE_FAIL_EDITING_PROHIBITED;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CrmReceivableApprovalServiceImplTest {

    @Mock
    private CrmReceivableMapper receivableMapper;
    @Mock
    private CrmNoRedisDAO noRedisDAO;
    @Mock
    private CrmContractService contractService;
    @Mock
    private CrmReceivablePlanService receivablePlanService;
    @Mock
    private CrmPermissionService permissionService;
    @Mock
    private AdminUserApi adminUserApi;
    @Mock
    private BpmProcessInstanceApi bpmProcessInstanceApi;

    @InjectMocks
    private CrmReceivableServiceImpl service;

    @Test
    void updateReceivableRejectsProcessingState() {
        when(receivableMapper.selectByIdForUpdate(10L))
                .thenReturn(receivable(CrmAuditStatusEnum.PROCESS, "process-1"));

        ServiceException exception = assertThrows(ServiceException.class,
                () -> service.updateReceivable(request()));

        assertEquals(RECEIVABLE_UPDATE_FAIL_EDITING_PROHIBITED.getCode(), exception.getCode());
        verify(receivableMapper, never()).updateById(any(CrmReceivableDO.class));
    }

    @Test
    void updateRejectedReceivableCreatesRevisionDraft() {
        when(receivableMapper.selectByIdForUpdate(10L))
                .thenReturn(receivable(CrmAuditStatusEnum.REJECT, "process-1"));
        stubAvailableAmount();

        service.updateReceivable(request());

        ArgumentCaptor<CrmReceivableDO> captor = ArgumentCaptor.forClass(CrmReceivableDO.class);
        verify(receivableMapper).updateById(captor.capture());
        assertEquals(CrmAuditStatusEnum.DRAFT.getStatus(), captor.getValue().getAuditStatus());
        assertEquals(20L, captor.getValue().getContractId());
        assertEquals(30L, captor.getValue().getCustomerId());
        assertEquals(40L, captor.getValue().getOwnerUserId());
    }

    @Test
    void updateCanceledReceivableCreatesRevisionDraft() {
        when(receivableMapper.selectByIdForUpdate(10L))
                .thenReturn(receivable(CrmAuditStatusEnum.CANCEL, "process-1"));
        stubAvailableAmount();

        service.updateReceivable(request());

        ArgumentCaptor<CrmReceivableDO> captor = ArgumentCaptor.forClass(CrmReceivableDO.class);
        verify(receivableMapper).updateById(captor.capture());
        assertEquals(CrmAuditStatusEnum.DRAFT.getStatus(), captor.getValue().getAuditStatus());
    }

    @Test
    void submitRevisedDraftStartsNewProcess() {
        CrmReceivableDO draft = receivable(CrmAuditStatusEnum.DRAFT, "process-1");
        when(receivableMapper.selectById(10L)).thenReturn(draft);
        when(receivableMapper.selectContractIdForUpdate(20L)).thenReturn(20L);
        when(receivableMapper.selectByIdForUpdate(10L)).thenReturn(draft);
        stubAvailableAmount();
        when(bpmProcessInstanceApi.createProcessInstance(any(), any(BpmProcessInstanceCreateReqDTO.class)))
                .thenReturn("process-2");

        service.submitReceivable(10L, 1L);

        ArgumentCaptor<CrmReceivableDO> captor = ArgumentCaptor.forClass(CrmReceivableDO.class);
        verify(receivableMapper).updateById(captor.capture());
        assertEquals("process-2", captor.getValue().getProcessInstanceId());
        assertEquals(CrmAuditStatusEnum.PROCESS.getStatus(), captor.getValue().getAuditStatus());
    }

    @Test
    void createAndSubmitRequireObjectWritePermission() throws NoSuchMethodException {
        CrmPermission createPermission = CrmReceivableServiceImpl.class
                .getMethod("createReceivable", CrmReceivableSaveReqVO.class)
                .getAnnotation(CrmPermission.class);
        assertEquals(CrmBizTypeEnum.CRM_CONTRACT, createPermission.bizType()[0]);
        assertEquals("#createReqVO.contractId", createPermission.bizId());
        assertEquals(CrmPermissionLevelEnum.WRITE, createPermission.level());

        CrmPermission submitPermission = CrmReceivableServiceImpl.class
                .getMethod("submitReceivable", Long.class, Long.class)
                .getAnnotation(CrmPermission.class);
        assertEquals(CrmBizTypeEnum.CRM_RECEIVABLE, submitPermission.bizType()[0]);
        assertEquals("#id", submitPermission.bizId());
        assertEquals(CrmPermissionLevelEnum.WRITE, submitPermission.level());
    }

    @Test
    void auditCancelMapsToCrmStatus() {
        when(receivableMapper.selectById(10L))
                .thenReturn(receivable(CrmAuditStatusEnum.PROCESS, "process-2"));
        when(receivableMapper.updateAuditStatusIfProcessing(
                10L, "process-2", CrmAuditStatusEnum.CANCEL.getStatus())).thenReturn(1);

        service.updateReceivableAuditStatus(
                10L, "process-2", BpmProcessInstanceStatusEnum.CANCEL.getStatus());

        verify(receivableMapper).updateAuditStatusIfProcessing(
                10L, "process-2", CrmAuditStatusEnum.CANCEL.getStatus());
    }

    @Test
    void duplicateAuditEventIsIdempotent() {
        when(receivableMapper.selectById(10L))
                .thenReturn(receivable(CrmAuditStatusEnum.REJECT, "process-2"));

        service.updateReceivableAuditStatus(
                10L, "process-2", BpmProcessInstanceStatusEnum.REJECT.getStatus());

        verify(receivableMapper, never()).updateAuditStatusIfProcessing(any(), any(), any());
    }

    @Test
    void staleAuditEventDoesNotOverwriteCurrentProcess() {
        when(receivableMapper.selectById(10L))
                .thenReturn(receivable(CrmAuditStatusEnum.PROCESS, "process-2"));

        service.updateReceivableAuditStatus(
                10L, "process-1", BpmProcessInstanceStatusEnum.APPROVE.getStatus());

        verify(receivableMapper, never()).updateAuditStatusIfProcessing(any(), any(), any());
    }

    @Test
    void deleteReceivableRejectsApprovalHistory() {
        when(receivableMapper.selectByIdForUpdate(10L))
                .thenReturn(receivable(CrmAuditStatusEnum.REJECT, "process-1"));

        ServiceException exception = assertThrows(ServiceException.class, () -> service.deleteReceivable(10L));

        assertEquals(RECEIVABLE_DELETE_FAIL_NOT_NEW_DRAFT.getCode(), exception.getCode());
        verify(receivableMapper, never()).deleteById(10L);
    }

    @Test
    void deleteReceivableAllowsUnplannedNewDraft() {
        when(receivableMapper.selectByIdForUpdate(10L))
                .thenReturn(receivable(CrmAuditStatusEnum.DRAFT, null));

        service.deleteReceivable(10L);

        verify(receivableMapper).deleteById(10L);
        verify(permissionService).deletePermission(CrmBizTypeEnum.CRM_RECEIVABLE.getType(), 10L);
    }

    @Test
    void deleteReceivableRejectsPlanLinkedDraft() {
        when(receivableMapper.selectByIdForUpdate(10L))
                .thenReturn(receivable(CrmAuditStatusEnum.DRAFT, null).setPlanId(5L));

        ServiceException exception = assertThrows(ServiceException.class, () -> service.deleteReceivable(10L));

        assertEquals(RECEIVABLE_DELETE_FAIL_NOT_NEW_DRAFT.getCode(), exception.getCode());
        verify(receivableMapper, never()).deleteById(10L);
    }

    private void stubAvailableAmount() {
        when(contractService.validateContract(20L)).thenReturn(new CrmContractDO()
                .setId(20L).setAuditStatus(CrmAuditStatusEnum.APPROVE.getStatus())
                .setTotalPrice(new BigDecimal("100.00")));
        when(receivableMapper.selectListByContractIdAndStatus(any(), any()))
                .thenReturn(Collections.emptyList());
    }

    private static CrmReceivableSaveReqVO request() {
        return new CrmReceivableSaveReqVO()
                .setId(10L)
                .setContractId(999L)
                .setCustomerId(999L)
                .setOwnerUserId(999L)
                .setPrice(new BigDecimal("50.00"))
                .setReturnTime(LocalDateTime.of(2026, 7, 14, 0, 0));
    }

    private static CrmReceivableDO receivable(CrmAuditStatusEnum status, String processInstanceId) {
        return new CrmReceivableDO()
                .setId(10L)
                .setNo("HK-10")
                .setContractId(20L)
                .setCustomerId(30L)
                .setOwnerUserId(40L)
                .setPrice(new BigDecimal("50.00"))
                .setAuditStatus(status.getStatus())
                .setProcessInstanceId(processInstanceId);
    }

}
