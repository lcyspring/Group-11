package com.meession.etm.module.crm.controller.admin.activity;

import com.meession.etm.framework.common.pojo.PageResult;
import com.meession.etm.framework.security.core.LoginUser;
import com.meession.etm.module.crm.controller.admin.activity.vo.*;
import com.meession.etm.module.crm.dal.dataobject.activity.*;
import com.meession.etm.module.crm.service.activity.CrmActivityService;
import com.meession.etm.module.system.api.user.AdminUserApi;
import com.meession.etm.module.system.api.user.dto.AdminUserRespDTO;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CrmActivityControllerTest {

    @Mock private CrmActivityService activityService;
    @Mock private AdminUserApi adminUserApi;
    @InjectMocks private CrmActivityController controller;

    @BeforeEach
    void setLoginUser() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(new LoginUser().setId(7L), null));
    }

    @AfterEach
    void clearLoginUser() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void taskCommandsPassCurrentUserToService() {
        CrmTaskSaveReqVO save = new CrmTaskSaveReqVO().setId(1L);
        CrmTaskActionReqVO action = new CrmTaskActionReqVO().setId(1L).setRemark("ok");
        when(activityService.createTask(save, 7L)).thenReturn(1L);

        assertEquals(1L, controller.createTask(save).getData());
        assertTrue(controller.updateTask(save).getData());
        assertTrue(controller.startTask(action).getData());
        assertTrue(controller.completeTask(action).getData());
        assertTrue(controller.markTaskUnable(action).getData());
        assertTrue(controller.cancelTask(action).getData());
        verify(activityService).updateTask(save, 7L);
        verify(activityService).startTask(action, 7L);
        verify(activityService).completeTask(action, 7L);
        verify(activityService).markTaskUnable(action, 7L);
        verify(activityService).cancelTask(action, 7L);
    }

    @Test
    void taskPageAndHistoryResolveAllUserNames() {
        CrmTaskDO task = new CrmTaskDO().setId(1L).setAssigneeUserId(8L);
        task.setCreator("9");
        when(activityService.getTaskPage(any())).thenReturn(new PageResult<>(List.of(task), 1L));
        when(activityService.getTaskActionRecords(1L, 7L)).thenReturn(List.of(
                new CrmTaskActionRecordDO().setId(2L).setTaskId(1L).setOperatorUserId(7L)));
        when(adminUserApi.getUserMap(any())).thenReturn(Map.of(
                7L, user(7L, "操作人"), 8L, user(8L, "负责人"), 9L, user(9L, "创建人")));

        CrmTaskRespVO response = controller.getTaskPage(new CrmTaskPageReqVO()).getData().getList().get(0);
        assertEquals("负责人", response.getAssigneeUserName());
        assertEquals("创建人", response.getCreatorName());
        assertEquals("操作人", controller.getTaskActionRecords(1L).getData().get(0).getOperatorUserName());
    }

    @Test
    void callAndSmsPagesResolveOperatorNamesAndCreateWithCurrentUser() {
        CrmCallRecordDO call = new CrmCallRecordDO().setId(1L).setOperatorUserId(7L);
        CrmSmsRecordDO sms = new CrmSmsRecordDO().setId(2L).setOperatorUserId(7L);
        when(activityService.getCallRecordPage(any())).thenReturn(new PageResult<>(List.of(call), 1L));
        when(activityService.getSmsRecordPage(any())).thenReturn(new PageResult<>(List.of(sms), 1L));
        when(adminUserApi.getUserMap(any())).thenReturn(Map.of(7L, user(7L, "操作人")));
        CrmCallRecordSaveReqVO callRequest = new CrmCallRecordSaveReqVO();
        CrmSmsRecordSaveReqVO smsRequest = new CrmSmsRecordSaveReqVO();
        when(activityService.createCallRecord(callRequest, 7L)).thenReturn(1L);
        when(activityService.createSmsRecord(smsRequest, 7L)).thenReturn(2L);

        assertEquals("操作人", controller.getCallRecordPage(new CrmActivityPageReqVO())
                .getData().getList().get(0).getOperatorUserName());
        assertEquals("操作人", controller.getSmsRecordPage(new CrmActivityPageReqVO())
                .getData().getList().get(0).getOperatorUserName());
        assertEquals(1L, controller.createCallRecord(callRequest).getData());
        assertEquals(2L, controller.createSmsRecord(smsRequest).getData());
    }

    @Test
    void conversionRecordHandlesMissingAndHistoricalOperator() {
        when(activityService.getConversionRecord(1L)).thenReturn(null);
        assertNull(controller.getConversionRecord(1L).getData());

        CrmClueConversionRecordDO record = new CrmClueConversionRecordDO().setId(3L).setClueId(2L)
                .setOperatorUserId(null).setConvertedAt(LocalDateTime.now());
        when(activityService.getConversionRecord(2L)).thenReturn(record);
        CrmClueConversionRecordRespVO response = controller.getConversionRecord(2L).getData();
        assertEquals(3L, response.getId());
        assertNull(response.getOperatorUserName());
        verifyNoInteractions(adminUserApi);
    }

    @Test
    void emptyPagesSkipUserLookup() {
        when(activityService.getTaskPage(any())).thenReturn(PageResult.empty());
        controller.getTaskPage(new CrmTaskPageReqVO());
        verifyNoInteractions(adminUserApi);
    }

    private static AdminUserRespDTO user(Long id, String name) {
        return new AdminUserRespDTO().setId(id).setNickname(name);
    }
}
