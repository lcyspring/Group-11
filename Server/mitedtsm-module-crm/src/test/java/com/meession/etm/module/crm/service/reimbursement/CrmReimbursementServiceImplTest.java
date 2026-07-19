package com.meession.etm.module.crm.service.reimbursement;

import com.meession.etm.module.bpm.api.task.BpmProcessInstanceApi;
import com.meession.etm.module.bpm.enums.task.BpmProcessInstanceStatusEnum;
import com.meession.etm.module.crm.controller.admin.reimbursement.vo.CrmReimbursementItemSaveReqVO;
import com.meession.etm.module.crm.controller.admin.reimbursement.vo.CrmReimbursementSaveReqVO;
import com.meession.etm.module.crm.dal.dataobject.contract.CrmContractDO;
import com.meession.etm.module.crm.dal.dataobject.reimbursement.*;
import com.meession.etm.module.crm.dal.mysql.contract.CrmContractMapper;
import com.meession.etm.module.crm.dal.mysql.customer.CrmCustomerMapper;
import com.meession.etm.module.crm.dal.mysql.reimbursement.*;
import com.meession.etm.module.crm.dal.redis.no.CrmNoRedisDAO;
import com.meession.etm.module.crm.enums.common.CrmAuditStatusEnum;
import com.meession.etm.module.crm.enums.common.CrmBizTypeEnum;
import com.meession.etm.module.crm.framework.reimbursement.CrmReimbursementProperties;
import com.meession.etm.module.crm.service.permission.CrmPermissionService;
import com.meession.etm.module.system.api.user.AdminUserApi;
import com.meession.etm.module.system.api.user.dto.AdminUserRespDTO;
import com.meession.etm.module.infra.api.file.FileApi;
import com.meession.etm.module.infra.api.file.dto.FileRespDTO;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static com.meession.etm.framework.test.core.util.AssertUtils.assertServiceException;
import static com.meession.etm.module.crm.enums.ErrorCodeConstants.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CrmReimbursementServiceImplTest {
    @Mock private CrmReimbursementMapper reimbursementMapper;
    @Mock private CrmReimbursementItemMapper itemMapper;
    @Mock private CrmReimbursementActionRecordMapper actionMapper;
    @Mock private CrmExpenseCategoryMapper categoryMapper;
    @Mock private CrmCustomerMapper customerMapper;
    @Mock private CrmContractMapper contractMapper;
    @Mock private CrmNoRedisDAO noRedisDAO;
    @Mock private CrmPermissionService permissionService;
    @Mock private BpmProcessInstanceApi bpmProcessInstanceApi;
    @Mock private AdminUserApi adminUserApi;
    @Mock private FileApi fileApi;
    @Mock private CrmReimbursementProperties properties;
    @InjectMocks private CrmReimbursementServiceImpl service;

    @Test
    void createCalculatesTotalAndSnapshotsApplicant() {
        AdminUserRespDTO applicant = new AdminUserRespDTO().setId(7L).setDeptId(3L);
        when(adminUserApi.getUser(7L)).thenReturn(applicant);
        when(categoryMapper.selectByIds(anyCollection())).thenReturn(List.of(category(1L, 0)));
        when(properties.getNumberPrefix()).thenReturn("BX");
        when(properties.getDefaultCurrency()).thenReturn("CNY");
        when(noRedisDAO.generateMonthly("BX")).thenReturn("BX202607-0001");
        doAnswer(invocation -> { ((CrmReimbursementDO) invocation.getArgument(0)).setId(9L); return 1; })
                .when(reimbursementMapper).insert(any(CrmReimbursementDO.class));

        Long id = service.createReimbursement(request("12.340000", "0.660000"), 7L);

        assertEquals(9L, id);
        ArgumentCaptor<CrmReimbursementDO> inserted = ArgumentCaptor.forClass(CrmReimbursementDO.class);
        verify(reimbursementMapper).insert(inserted.capture());
        assertEquals(new BigDecimal("13.000000"), inserted.getValue().getTotalAmount());
        assertEquals(7L, inserted.getValue().getApplicantUserId());
        assertEquals(3L, inserted.getValue().getDepartmentId());
        assertEquals("CNY", inserted.getValue().getCurrency());
        verify(permissionService).createPermission(argThat(permission ->
                permission.getBizType().equals(CrmBizTypeEnum.CRM_REIMBURSEMENT.getType())
                        && permission.getBizId().equals(9L) && permission.getUserId().equals(7L)));
        verify(actionMapper).insert(org.mockito.ArgumentMatchers.<CrmReimbursementActionRecordDO>argThat(
                action -> action.getAmountSnapshot().compareTo(new BigDecimal("13")) == 0));
    }

    @Test
    void createRejectsDisabledCategory() {
        when(adminUserApi.getUser(7L)).thenReturn(new AdminUserRespDTO().setId(7L));
        when(categoryMapper.selectByIds(anyCollection())).thenReturn(List.of(category(1L, 1)));

        assertServiceException(() -> service.createReimbursement(request("10.00"), 7L),
                REIMBURSEMENT_CATEGORY_DISABLED, "差旅费");
        verify(reimbursementMapper, never()).insert(any(CrmReimbursementDO.class));
    }

    @Test
    void createRejectsItemOutsideExpenseRange() {
        CrmReimbursementSaveReqVO request = request("10.00");
        request.getItems().get(0).setOccurredDate(LocalDate.of(2026, 7, 20));
        when(adminUserApi.getUser(7L)).thenReturn(new AdminUserRespDTO().setId(7L));

        assertServiceException(() -> service.createReimbursement(request, 7L), REIMBURSEMENT_ITEM_DATE_INVALID);
    }

    @Test
    void createRejectsContractCustomerMismatch() {
        CrmReimbursementSaveReqVO request = request("10.00").setCustomerId(30L).setContractId(20L);
        when(adminUserApi.getUser(7L)).thenReturn(new AdminUserRespDTO().setId(7L));
        when(contractMapper.selectById(20L)).thenReturn(new CrmContractDO().setId(20L).setCustomerId(31L));
        when(permissionService.hasPermission(anyInt(), anyLong(), eq(7L), any())).thenReturn(true);

        assertServiceException(() -> service.createReimbursement(request, 7L),
                REIMBURSEMENT_CONTRACT_CUSTOMER_MISMATCH);
    }

    @Test
    void updateRejectedRecordReturnsToDraftAndReplacesItems() {
        CrmReimbursementDO old = reimbursement(CrmAuditStatusEnum.REJECT).setVersion(2);
        when(reimbursementMapper.selectByIdForUpdate(9L)).thenReturn(old);
        when(categoryMapper.selectByIds(anyCollection())).thenReturn(List.of(category(1L, 0)));
        when(reimbursementMapper.updateContentIfVersion(any(CrmReimbursementDO.class), eq(2))).thenReturn(1);
        CrmReimbursementSaveReqVO request = request("20.00").setId(9L);

        service.updateReimbursement(request, 7L);

        verify(reimbursementMapper).updateContentIfVersion(org.mockito.ArgumentMatchers.<CrmReimbursementDO>argThat(update ->
                update.getAuditStatus().equals(CrmAuditStatusEnum.DRAFT.getStatus())
                        && update.getVersion().equals(2)
                        && update.getTotalAmount().compareTo(new BigDecimal("20")) == 0), eq(2));
        verify(itemMapper).deleteByReimbursementId(9L);
        verify(actionMapper).insert(org.mockito.ArgumentMatchers.<CrmReimbursementActionRecordDO>argThat(
                action -> action.getFromStatus().equals(30) && action.getToStatus().equals(0)));
    }

    @Test
    void submitUsesPersistedDetailTotal() {
        CrmReimbursementDO draft = reimbursement(CrmAuditStatusEnum.DRAFT).setVersion(1);
        when(reimbursementMapper.selectByIdForUpdate(9L)).thenReturn(draft);
        when(itemMapper.selectListByReimbursementId(9L)).thenReturn(List.of(
                new CrmReimbursementItemDO().setAmount(new BigDecimal("13.00"))));
        when(properties.getProcessDefinitionKey()).thenReturn("crm-reimbursement-audit");
        when(bpmProcessInstanceApi.createProcessInstance(eq(7L), any())).thenReturn("process-1");
        when(reimbursementMapper.submitIfDraftAndVersion(9L, 1, "process-1")).thenReturn(1);

        service.submitReimbursement(9L, 7L);

        verify(reimbursementMapper).submitIfDraftAndVersion(9L, 1, "process-1");
        verify(actionMapper).insert(org.mockito.ArgumentMatchers.<CrmReimbursementActionRecordDO>argThat(
                action -> action.getActionType().equals(3)));
    }

    @Test
    void callbackIsIdempotentAndIgnoresOldProcess() {
        CrmReimbursementDO processing = reimbursement(CrmAuditStatusEnum.PROCESS).setProcessInstanceId("process-1");
        when(reimbursementMapper.selectById(9L)).thenReturn(processing);
        when(reimbursementMapper.updateAuditStatusIfProcessing(9L, "process-1", 20)).thenReturn(1);

        service.updateAuditStatus(9L, "process-1", BpmProcessInstanceStatusEnum.APPROVE.getStatus());
        processing.setAuditStatus(20);
        service.updateAuditStatus(9L, "process-1", BpmProcessInstanceStatusEnum.APPROVE.getStatus());
        service.updateAuditStatus(9L, "old-process", BpmProcessInstanceStatusEnum.REJECT.getStatus());

        verify(actionMapper, times(1)).insert(org.mockito.ArgumentMatchers.<CrmReimbursementActionRecordDO>argThat(
                action -> action.getActionType().equals(4)
                        && action.getFromStatus().equals(10) && action.getToStatus().equals(20)));
    }

    @Test
    void deleteRejectsPreviouslySubmittedDraft() {
        when(reimbursementMapper.selectByIdForUpdate(9L)).thenReturn(reimbursement(CrmAuditStatusEnum.DRAFT));
        when(actionMapper.selectCountByReimbursementIdAndAction(9L, 3)).thenReturn(1L);

        assertServiceException(() -> service.deleteReimbursement(9L, 7L),
                REIMBURSEMENT_DELETE_STATUS_INVALID);
        verify(reimbursementMapper, never()).deleteById(anyLong());
    }

    @Test
    void uploadUsesObjectScopedProtectedDirectory() {
        when(reimbursementMapper.selectById(9L)).thenReturn(reimbursement(CrmAuditStatusEnum.DRAFT));
        when(properties.getProtectedFileDirectory()).thenReturn("crm-protected/reimbursement");
        when(fileApi.createFile(any(), eq("receipt.pdf"), eq("crm-protected/reimbursement/9"),
                eq("application/pdf"))).thenReturn("http://files/receipt.pdf");

        assertEquals("http://files/receipt.pdf", service.uploadAttachmentFile(
                9L, new byte[]{1}, "receipt.pdf", "application/pdf"));
    }

    @Test
    void updateRejectsAttachmentFromAnotherReimbursement() {
        CrmReimbursementDO old = reimbursement(CrmAuditStatusEnum.DRAFT).setVersion(2);
        when(reimbursementMapper.selectByIdForUpdate(9L)).thenReturn(old);
        when(categoryMapper.selectByIds(anyCollection())).thenReturn(List.of(category(1L, 0)));
        when(properties.getProtectedFileDirectory()).thenReturn("crm-protected/reimbursement");
        when(fileApi.getFileByUrl("http://files/foreign.pdf")).thenReturn(
                new FileRespDTO().setPath("crm-protected/reimbursement/8/foreign.pdf"));
        CrmReimbursementSaveReqVO request = request("20.00").setId(9L);
        request.getItems().get(0).setAttachmentUrls(List.of("http://files/foreign.pdf"));

        assertServiceException(() -> service.updateReimbursement(request, 7L),
                REIMBURSEMENT_ATTACHMENT_NOT_PROTECTED);
        verify(reimbursementMapper, never()).updateContentIfVersion(any(CrmReimbursementDO.class), anyInt());
    }

    private static CrmReimbursementSaveReqVO request(String... amounts) {
        List<CrmReimbursementItemSaveReqVO> items = java.util.Arrays.stream(amounts)
                .map(amount -> new CrmReimbursementItemSaveReqVO().setCategoryId(1L)
                        .setOccurredDate(LocalDate.of(2026, 7, 10)).setAmount(new BigDecimal(amount))
                        .setDescription("客户现场差旅费用")).toList();
        return new CrmReimbursementSaveReqVO().setExpenseStartDate(LocalDate.of(2026, 7, 1))
                .setExpenseEndDate(LocalDate.of(2026, 7, 15)).setReason("客户项目差旅费用报销")
                .setItems(items);
    }

    private static CrmExpenseCategoryDO category(Long id, Integer status) {
        return new CrmExpenseCategoryDO().setId(id).setName("差旅费").setStatus(status);
    }

    private static CrmReimbursementDO reimbursement(CrmAuditStatusEnum status) {
        return new CrmReimbursementDO().setId(9L).setTotalAmount(new BigDecimal("13.00"))
                .setAuditStatus(status.getStatus()).setReason("客户项目差旅费用报销").setVersion(0);
    }
}
