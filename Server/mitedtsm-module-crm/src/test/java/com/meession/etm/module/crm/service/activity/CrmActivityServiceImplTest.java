package com.meession.etm.module.crm.service.activity;

import com.meession.etm.module.crm.controller.admin.activity.vo.*;
import com.meession.etm.module.crm.dal.dataobject.activity.*;
import com.meession.etm.module.crm.dal.dataobject.clue.CrmClueDO;
import com.meession.etm.module.crm.dal.dataobject.customer.CrmCustomerDO;
import com.meession.etm.module.crm.dal.mysql.activity.*;
import com.meession.etm.module.crm.dal.mysql.clue.CrmClueMapper;
import com.meession.etm.module.crm.dal.mysql.contact.CrmContactMapper;
import com.meession.etm.module.crm.dal.mysql.customer.CrmCustomerMapper;
import com.meession.etm.module.crm.enums.activity.*;
import com.meession.etm.module.crm.enums.clue.CrmCluePoolStatusEnum;
import com.meession.etm.module.crm.enums.common.CrmBizTypeEnum;
import com.meession.etm.module.crm.enums.permission.CrmPermissionLevelEnum;
import com.meession.etm.module.crm.framework.activity.CrmActivityProperties;
import com.meession.etm.module.crm.service.permission.CrmPermissionService;
import com.meession.etm.module.system.api.user.AdminUserApi;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;

import static com.meession.etm.framework.test.core.util.AssertUtils.assertServiceException;
import static com.meession.etm.module.crm.enums.ErrorCodeConstants.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CrmActivityServiceImplTest {

    @Mock private CrmTaskMapper taskMapper;
    @Mock private CrmTaskActionRecordMapper taskActionRecordMapper;
    @Mock private CrmCallRecordMapper callRecordMapper;
    @Mock private CrmSmsRecordMapper smsRecordMapper;
    @Mock private CrmClueConversionRecordMapper conversionRecordMapper;
    @Mock private CrmClueMapper clueMapper;
    @Mock private CrmCustomerMapper customerMapper;
    @Mock private CrmContactMapper contactMapper;
    @Mock private CrmPermissionService permissionService;
    @Mock private AdminUserApi adminUserApi;
    @Mock private CrmActivityProperties properties;
    @Mock private CrmActivityProperties.TaskOverdue taskOverdue;
    @Mock private CrmActivityNotificationService notificationService;
    @InjectMocks private CrmActivityServiceImpl service;

    @Test
    void createTaskWritesInitialStateAndImmutableRecord() {
        when(clueMapper.selectByIdForUpdate(10L)).thenReturn(ownedClue());
        doAnswer(invocation -> {
            ((CrmTaskDO) invocation.getArgument(0)).setId(100L);
            return 1;
        }).when(taskMapper).insert(org.mockito.ArgumentMatchers.<CrmTaskDO>any());

        Long id = service.createTask(taskRequest(), 1L);

        assertEquals(100L, id);
        ArgumentCaptor<CrmTaskDO> task = ArgumentCaptor.forClass(CrmTaskDO.class);
        verify(taskMapper).insert(task.capture());
        assertEquals(CrmTaskStatusEnum.NOT_STARTED.getStatus(), task.getValue().getStatus());
        assertEquals("1", task.getValue().getCreator());
        assertNull(task.getValue().getSourceClueId());
        verify(taskActionRecordMapper).insert(org.mockito.ArgumentMatchers.<CrmTaskActionRecordDO>argThat(record ->
                record.getTaskId().equals(100L) && record.getActionType().equals(1)
                        && record.getFromStatus() == null && record.getToStatus().equals(0)));
        verify(notificationService).notifyAssigned(task.getValue());
    }

    @Test
    void createTaskRejectsPastDueTimeBeforeInsert() {
        when(clueMapper.selectByIdForUpdate(10L)).thenReturn(ownedClue());
        CrmTaskSaveReqVO request = taskRequest().setDueTime(LocalDateTime.now().minusMinutes(1));

        assertServiceException(() -> service.createTask(request, 1L), TASK_TIME_INVALID);

        verify(taskMapper, never()).insert(org.mockito.ArgumentMatchers.<CrmTaskDO>any());
    }

    @Test
    void assigneeCompletesTaskAndRecordKeepsOriginalStatus() {
        CrmTaskDO task = task(100L, CrmTaskStatusEnum.IN_PROGRESS, 2L);
        when(taskMapper.selectByIdForUpdate(100L)).thenReturn(task);
        when(permissionService.hasPermission(1, 10L, 2L, CrmPermissionLevelEnum.WRITE)).thenReturn(true);
        when(taskMapper.finishIfOpen(eq(100L), eq(20), eq("完成拜访"), eq(2L), any())).thenReturn(1);

        service.completeTask(new CrmTaskActionReqVO().setId(100L).setRemark("完成拜访"), 2L);

        verify(taskActionRecordMapper).insert(org.mockito.ArgumentMatchers.<CrmTaskActionRecordDO>argThat(record ->
                record.getActionType().equals(4) && record.getFromStatus().equals(10)
                        && record.getToStatus().equals(20)));
        verify(notificationService).notifyFinished(argThat(finished ->
                finished.getStatus().equals(20) && "完成拜访".equals(finished.getResult())));
    }

    @Test
    void nonAssigneeCannotProcessTask() {
        when(taskMapper.selectByIdForUpdate(100L)).thenReturn(task(100L, CrmTaskStatusEnum.NOT_STARTED, 2L));
        when(permissionService.hasPermission(1, 10L, 3L, CrmPermissionLevelEnum.WRITE)).thenReturn(true);

        assertServiceException(() -> service.startTask(new CrmTaskActionReqVO().setId(100L), 3L),
                TASK_ASSIGNEE_ONLY);

        verify(taskMapper, never()).startIfStartable(anyLong(), anyLong(), any());
    }

    @Test
    void overdueTaskRemainsStartableAndTransitionIsAudited() {
        CrmTaskDO task = task(100L, CrmTaskStatusEnum.OVERDUE, 2L);
        when(taskMapper.selectByIdForUpdate(100L)).thenReturn(task);
        when(permissionService.hasPermission(1, 10L, 2L, CrmPermissionLevelEnum.WRITE)).thenReturn(true);
        when(taskMapper.startIfStartable(eq(100L), eq(2L), any())).thenReturn(1);

        service.startTask(new CrmTaskActionReqVO().setId(100L).setRemark("逾期后补办"), 2L);

        verify(taskActionRecordMapper).insert(org.mockito.ArgumentMatchers.<CrmTaskActionRecordDO>argThat(record ->
                record.getActionType().equals(3) && record.getFromStatus().equals(50)
                        && record.getToStatus().equals(10)));
    }

    @Test
    void schedulerMarksOverdueTasksInConfiguredBatches() {
        CrmTaskDO task = task(100L, CrmTaskStatusEnum.NOT_STARTED, 2L);
        when(properties.getTaskOverdue()).thenReturn(taskOverdue);
        when(taskOverdue.isEnabled()).thenReturn(true);
        when(taskOverdue.getMaxBatches()).thenReturn(20);
        when(taskOverdue.getBatchSize()).thenReturn(500);
        when(taskOverdue.getMaxBatchSize()).thenReturn(5000);
        when(taskMapper.selectOverdueCandidates(eq(0L), any(), eq(500), eq(5000))).thenReturn(List.of(task));
        when(taskMapper.markOverdueIfOpen(eq(100L), any())).thenReturn(1);

        assertEquals(1, service.markOverdueTasks());

        verify(taskActionRecordMapper).insert(org.mockito.ArgumentMatchers.<CrmTaskActionRecordDO>argThat(record ->
                record.getActionType().equals(7) && record.getFromStatus().equals(0)
                        && record.getToStatus().equals(50) && record.getOperatorUserId() == null));
    }

    @Test
    void connectedCallCalculatesDurationAndRequiresProtectedRecording() {
        when(customerMapper.selectById(10L)).thenReturn(new CrmCustomerDO().setId(10L));
        when(properties.getProtectedCallRecordingDirectory()).thenReturn("crm-protected/call");
        when(properties.getMaxCallDurationSeconds()).thenReturn(86400);
        LocalDateTime start = LocalDateTime.of(2026, 7, 15, 9, 0);
        CrmCallRecordSaveReqVO request = new CrmCallRecordSaveReqVO().setBizType(2).setBizId(10L)
                .setDirection(1).setStatus(10).setPhone("13800138000").setStartTime(start)
                .setEndTime(start.plusSeconds(65)).setRecordingUrl("crm-protected/call/record-1.mp3");

        service.createCallRecord(request, 1L);

        verify(callRecordMapper).insert(org.mockito.ArgumentMatchers.<CrmCallRecordDO>argThat(record ->
                record.getDurationSeconds().equals(65)
                        && "crm-protected/call/record-1.mp3".equals(record.getRecordingUrl())));

        request.setRecordingUrl("public/record-1.mp3");
        assertServiceException(() -> service.createCallRecord(request, 1L), CALL_RECORDING_PATH_INVALID);
    }

    @Test
    void connectedCallRejectsConfiguredDurationOverflow() {
        when(customerMapper.selectById(10L)).thenReturn(new CrmCustomerDO().setId(10L));
        when(properties.getMaxCallDurationSeconds()).thenReturn(60);
        LocalDateTime start = LocalDateTime.of(2026, 7, 15, 9, 0);
        CrmCallRecordSaveReqVO request = new CrmCallRecordSaveReqVO().setBizType(2).setBizId(10L)
                .setDirection(1).setStatus(10).setPhone("13800138000").setStartTime(start)
                .setEndTime(start.plusSeconds(61));

        assertServiceException(() -> service.createCallRecord(request, 1L), CALL_TIME_INVALID);

        verify(callRecordMapper, never()).insert(org.mockito.ArgumentMatchers.<CrmCallRecordDO>any());
    }

    @Test
    void smsDirectionAndStatusMustAgree() {
        when(customerMapper.selectById(10L)).thenReturn(new CrmCustomerDO().setId(10L));
        CrmSmsRecordSaveReqVO request = new CrmSmsRecordSaveReqVO().setBizType(2).setBizId(10L)
                .setDirection(CrmSmsDirectionEnum.INBOUND.getDirection())
                .setStatus(CrmSmsStatusEnum.SENT.getStatus()).setMobile("13800138000")
                .setContent("客户回复").setOccurredTime(LocalDateTime.now());

        assertServiceException(() -> service.createSmsRecord(request, 1L), SMS_STATUS_INVALID);

        verify(smsRecordMapper, never()).insert(org.mockito.ArgumentMatchers.<CrmSmsRecordDO>any());
    }

    @Test
    void migrationMovesAllActivitiesAndWritesOneAudit() {
        List<CrmTaskDO> tasks = List.of(task(100L, CrmTaskStatusEnum.NOT_STARTED, 2L),
                task(101L, CrmTaskStatusEnum.IN_PROGRESS, 2L));
        when(taskMapper.selectListByBiz(1, 10L)).thenReturn(tasks);
        when(taskMapper.migrateFromClue(10L, 20L, 1L)).thenReturn(2);
        when(callRecordMapper.migrateFromClue(10L, 20L, 1L)).thenReturn(1);
        when(smsRecordMapper.migrateFromClue(10L, 20L, 1L)).thenReturn(3);

        CrmClueConversionRecordDO record = service.migrateClueActivities(10L, 20L, 30L, 4, 1L);

        assertEquals(4, record.getFollowUpCount());
        assertEquals(2, record.getTaskCount());
        assertEquals(1, record.getCallCount());
        assertEquals(3, record.getSmsCount());
        verify(taskActionRecordMapper, times(2)).insert(
                org.mockito.ArgumentMatchers.<CrmTaskActionRecordDO>argThat(action ->
                        action.getActionType().equals(8) && action.getFromStatus().equals(action.getToStatus())));
        verify(conversionRecordMapper).insert(record);
    }

    @Test
    void duplicateConversionAuditStopsBeforeMigration() {
        when(conversionRecordMapper.selectByClueId(10L)).thenReturn(new CrmClueConversionRecordDO().setClueId(10L));

        assertServiceException(() -> service.migrateClueActivities(10L, 20L, 30L, 0, 1L),
                CLUE_CONVERSION_AUDIT_EXISTS);

        verify(taskMapper, never()).migrateFromClue(anyLong(), anyLong(), anyLong());
    }

    private static CrmTaskSaveReqVO taskRequest() {
        return new CrmTaskSaveReqVO().setBizType(CrmBizTypeEnum.CRM_CLUE.getType()).setBizId(10L)
                .setType(CrmTaskTypeEnum.FOLLOW_UP.getType()).setTitle("电话回访")
                .setPriority(CrmTaskPriorityEnum.HIGH.getPriority()).setAssigneeUserId(2L)
                .setDueTime(LocalDateTime.now().plusDays(1)).setNotifySystem(true);
    }

    private static CrmClueDO ownedClue() {
        return new CrmClueDO().setId(10L).setTransformStatus(false).setOwnerUserId(1L)
                .setPoolStatus(CrmCluePoolStatusEnum.OWNED.getStatus());
    }

    private static CrmTaskDO task(Long id, CrmTaskStatusEnum status, Long assignee) {
        CrmTaskDO task = new CrmTaskDO().setId(id).setBizType(CrmBizTypeEnum.CRM_CLUE.getType())
                .setBizId(10L).setType(1).setTitle("跟进任务").setStatus(status.getStatus())
                .setAssigneeUserId(assignee).setDueTime(LocalDateTime.now().plusDays(1));
        task.setCreator("1");
        return task;
    }
}
