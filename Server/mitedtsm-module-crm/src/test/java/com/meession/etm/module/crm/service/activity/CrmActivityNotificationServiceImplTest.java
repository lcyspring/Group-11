package com.meession.etm.module.crm.service.activity;

import com.meession.etm.module.crm.dal.dataobject.activity.CrmTaskDO;
import com.meession.etm.module.system.api.notify.NotifyMessageSendApi;
import com.meession.etm.module.system.api.notify.dto.NotifySendSingleToUserReqDTO;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CrmActivityNotificationServiceImplTest {

    @Mock private NotifyMessageSendApi notifyMessageSendApi;
    @InjectMocks private CrmActivityNotificationServiceImpl service;

    @Test
    void assignmentUsesConfiguredTemplateAndAssignee() {
        CrmTaskDO task = task().setNotifySystem(true).setAssigneeUserId(8L);

        service.notifyAssigned(task);

        ArgumentCaptor<NotifySendSingleToUserReqDTO> request =
                ArgumentCaptor.forClass(NotifySendSingleToUserReqDTO.class);
        verify(notifyMessageSendApi).sendSingleMessageToAdmin(request.capture());
        assertEquals(8L, request.getValue().getUserId());
        assertEquals("crm-task-assigned", request.getValue().getTemplateCode());
        assertEquals("拜访客户", request.getValue().getTemplateParams().get("title"));
    }

    @Test
    void disabledSystemNotificationDoesNotSend() {
        service.notifyAssigned(task().setNotifySystem(false).setAssigneeUserId(8L));
        verifyNoInteractions(notifyMessageSendApi);
    }

    @Test
    void completionNotifiesNumericCreator() {
        CrmTaskDO task = task().setResult("已签收");
        task.setCreator("9");

        service.notifyFinished(task);

        verify(notifyMessageSendApi).sendSingleMessageToAdmin(argThat(request ->
                request.getUserId().equals(9L) && request.getTemplateCode().equals("crm-task-finished")
                        && "已签收".equals(request.getTemplateParams().get("result"))));
    }

    @Test
    void nonNumericCreatorDoesNotSend() {
        CrmTaskDO task = task();
        task.setCreator("system-job");
        service.notifyFinished(task);
        verifyNoInteractions(notifyMessageSendApi);
    }

    private static CrmTaskDO task() {
        return new CrmTaskDO().setTitle("拜访客户").setDueTime(LocalDateTime.of(2026, 7, 16, 10, 0));
    }
}
