package com.meession.etm.module.bpm.service.message;

import com.meession.etm.framework.web.config.WebProperties;
import com.meession.etm.module.bpm.convert.message.BpmMessageConvert;
import com.meession.etm.module.bpm.enums.message.BpmMessageEnum;
import com.meession.etm.module.bpm.framework.message.BpmNotificationProperties;
import com.meession.etm.module.bpm.service.message.dto.BpmMessageSendWhenProcessInstanceApproveReqDTO;
import com.meession.etm.module.bpm.service.message.dto.BpmMessageSendWhenProcessInstanceRejectReqDTO;
import com.meession.etm.module.bpm.service.message.dto.BpmMessageSendWhenTaskCreatedReqDTO;
import com.meession.etm.module.bpm.service.message.dto.BpmMessageSendWhenTaskTimeoutReqDTO;
import com.meession.etm.module.system.api.sms.SmsSendApi;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import jakarta.annotation.Resource;
import java.util.HashMap;
import java.util.Map;

/**
 * BPM 消息 Service 实现类
 *
 * @author 密讯
 */
@Service
@Validated
@Slf4j
public class BpmMessageServiceImpl implements BpmMessageService {

    @Resource
    private SmsSendApi smsSendApi;

    @Resource
    private WebProperties webProperties;

    @Resource
    private BpmNotificationProperties notificationProperties;

    @Override
    public void sendMessageWhenProcessInstanceApprove(BpmMessageSendWhenProcessInstanceApproveReqDTO reqDTO) {
        Map<String, Object> templateParams = new HashMap<>();
        templateParams.put("processInstanceName", reqDTO.getProcessInstanceName());
        templateParams.put("detailUrl", getProcessInstanceDetailUrl(reqDTO.getProcessInstanceId()));
        sendSmsSafely(reqDTO.getStartUserId(), BpmMessageEnum.PROCESS_INSTANCE_APPROVE, templateParams);
    }

    @Override
    public void sendMessageWhenProcessInstanceReject(BpmMessageSendWhenProcessInstanceRejectReqDTO reqDTO) {
        Map<String, Object> templateParams = new HashMap<>();
        templateParams.put("processInstanceName", reqDTO.getProcessInstanceName());
        templateParams.put("reason", reqDTO.getReason());
        templateParams.put("detailUrl", getProcessInstanceDetailUrl(reqDTO.getProcessInstanceId()));
        sendSmsSafely(reqDTO.getStartUserId(), BpmMessageEnum.PROCESS_INSTANCE_REJECT, templateParams);
    }

    @Override
    public void sendMessageWhenTaskAssigned(BpmMessageSendWhenTaskCreatedReqDTO reqDTO) {
        Map<String, Object> templateParams = new HashMap<>();
        templateParams.put("processInstanceName", reqDTO.getProcessInstanceName());
        templateParams.put("taskName", reqDTO.getTaskName());
        templateParams.put("startUserNickname", reqDTO.getStartUserNickname());
        templateParams.put("detailUrl", getProcessInstanceDetailUrl(reqDTO.getProcessInstanceId()));
        sendSmsSafely(reqDTO.getAssigneeUserId(), BpmMessageEnum.TASK_ASSIGNED, templateParams);
    }

    @Override
    public void sendMessageWhenTaskTimeout(BpmMessageSendWhenTaskTimeoutReqDTO reqDTO) {
        Map<String, Object> templateParams = new HashMap<>();
        templateParams.put("processInstanceName", reqDTO.getProcessInstanceName());
        templateParams.put("taskName", reqDTO.getTaskName());
        templateParams.put("detailUrl", getProcessInstanceDetailUrl(reqDTO.getProcessInstanceId()));
        sendSmsSafely(reqDTO.getAssigneeUserId(), BpmMessageEnum.TASK_TIMEOUT, templateParams);
    }

    private void sendSmsSafely(Long userId, BpmMessageEnum messageType, Map<String, Object> templateParams) {
        if (!notificationProperties.isSmsEnabled()) {
            log.debug("[sendSmsSafely][BPM 短信通知未启用，跳过发送 templateCode={} userId={}]",
                    messageType.getSmsTemplateCode(), userId);
            return;
        }
        try {
            smsSendApi.sendSingleSmsToAdmin(BpmMessageConvert.INSTANCE.convert(userId,
                    messageType.getSmsTemplateCode(), templateParams));
        } catch (RuntimeException ex) {
            if (notificationProperties.isFailFast()) {
                throw ex;
            }
            log.warn("[sendSmsSafely][BPM 短信通知失败但不回滚审批 templateCode={} userId={} cause={}]",
                    messageType.getSmsTemplateCode(), userId, ex.getMessage(), ex);
        }
    }

    private String getProcessInstanceDetailUrl(String taskId) {
        return webProperties.getAdminUi().getUrl() + "/bpm/process-instance/detail?id=" + taskId;
    }

}
