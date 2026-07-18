package com.meession.etm.module.bpm.service.message;

import com.meession.etm.framework.web.config.WebProperties;
import com.meession.etm.module.bpm.enums.message.BpmMessageEnum;
import com.meession.etm.module.bpm.framework.message.BpmNotificationProperties;
import com.meession.etm.module.bpm.service.message.dto.BpmMessageSendWhenProcessInstanceApproveReqDTO;
import com.meession.etm.module.bpm.service.message.dto.BpmMessageSendWhenProcessInstanceRejectReqDTO;
import com.meession.etm.module.bpm.service.message.dto.BpmMessageSendWhenTaskCreatedReqDTO;
import com.meession.etm.module.bpm.service.message.dto.BpmMessageSendWhenTaskTimeoutReqDTO;
import com.meession.etm.module.system.api.sms.SmsSendApi;
import com.meession.etm.module.system.api.sms.dto.send.SmsSendSingleToUserReqDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class BpmMessageServiceImplTest {

    @Mock
    private SmsSendApi smsSendApi;

    private BpmMessageServiceImpl messageService;
    private BpmNotificationProperties properties;

    @BeforeEach
    void setUp() {
        WebProperties webProperties = new WebProperties();
        WebProperties.Ui adminUi = new WebProperties.Ui();
        adminUi.setUrl("https://crm.example.com");
        webProperties.setAdminUi(adminUi);
        properties = new BpmNotificationProperties();
        messageService = new BpmMessageServiceImpl();
        ReflectionTestUtils.setField(messageService, "smsSendApi", smsSendApi);
        ReflectionTestUtils.setField(messageService, "webProperties", webProperties);
        ReflectionTestUtils.setField(messageService, "notificationProperties", properties);
    }

    @Test
    void approvalDoesNotAttemptSmsWhenNotificationIsDisabled() {
        assertDoesNotThrow(() -> messageService.sendMessageWhenProcessInstanceApprove(approveRequest()));
        verify(smsSendApi, never()).sendSingleSmsToAdmin(any());
    }

    @Test
    void approvalUsesGovernedTemplateWhenSmsIsEnabled() {
        properties.setSmsEnabled(true);
        messageService.sendMessageWhenProcessInstanceApprove(approveRequest());

        ArgumentCaptor<SmsSendSingleToUserReqDTO> captor = ArgumentCaptor.forClass(SmsSendSingleToUserReqDTO.class);
        verify(smsSendApi).sendSingleSmsToAdmin(captor.capture());
        assertEquals(7L, captor.getValue().getUserId());
        assertEquals(BpmMessageEnum.PROCESS_INSTANCE_APPROVE.getSmsTemplateCode(),
                captor.getValue().getTemplateCode());
        assertEquals("合同审批", captor.getValue().getTemplateParams().get("processInstanceName"));
    }

    @Test
    void missingSmsTemplateDoesNotRollbackApprovalByDefault() {
        properties.setSmsEnabled(true);
        doThrow(new IllegalStateException("短信模板不存在"))
                .when(smsSendApi).sendSingleSmsToAdmin(any());

        assertDoesNotThrow(() -> messageService.sendMessageWhenProcessInstanceApprove(approveRequest()));
    }

    @Test
    void failFastPolicyCanExplicitlyPropagateProviderErrors() {
        properties.setSmsEnabled(true);
        properties.setFailFast(true);
        doThrow(new IllegalStateException("短信模板不存在"))
                .when(smsSendApi).sendSingleSmsToAdmin(any());

        assertThrows(IllegalStateException.class,
                () -> messageService.sendMessageWhenProcessInstanceApprove(approveRequest()));
    }

    @Test
    void allBpmSmsEventsUseTheirStableTemplateContracts() {
        properties.setSmsEnabled(true);
        messageService.sendMessageWhenProcessInstanceReject(new BpmMessageSendWhenProcessInstanceRejectReqDTO()
                .setProcessInstanceId("process-2").setProcessInstanceName("回款审批")
                .setStartUserId(8L).setReason("资料不完整"));
        messageService.sendMessageWhenTaskAssigned(new BpmMessageSendWhenTaskCreatedReqDTO()
                .setProcessInstanceId("process-3").setProcessInstanceName("报销审批")
                .setStartUserId(9L).setStartUserNickname("销售甲")
                .setTaskId("task-3").setTaskName("财务复核").setAssigneeUserId(10L));
        messageService.sendMessageWhenTaskTimeout(new BpmMessageSendWhenTaskTimeoutReqDTO()
                .setProcessInstanceId("process-4").setProcessInstanceName("退款审批")
                .setTaskId("task-4").setTaskName("主管审批").setAssigneeUserId(11L));

        ArgumentCaptor<SmsSendSingleToUserReqDTO> captor = ArgumentCaptor.forClass(SmsSendSingleToUserReqDTO.class);
        verify(smsSendApi, org.mockito.Mockito.times(3)).sendSingleSmsToAdmin(captor.capture());
        assertEquals(BpmMessageEnum.PROCESS_INSTANCE_REJECT.getSmsTemplateCode(),
                captor.getAllValues().get(0).getTemplateCode());
        assertEquals(BpmMessageEnum.TASK_ASSIGNED.getSmsTemplateCode(),
                captor.getAllValues().get(1).getTemplateCode());
        assertEquals(BpmMessageEnum.TASK_TIMEOUT.getSmsTemplateCode(),
                captor.getAllValues().get(2).getTemplateCode());
    }

    private static BpmMessageSendWhenProcessInstanceApproveReqDTO approveRequest() {
        return new BpmMessageSendWhenProcessInstanceApproveReqDTO()
                .setProcessInstanceId("process-1")
                .setProcessInstanceName("合同审批")
                .setStartUserId(7L);
    }

}
