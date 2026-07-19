package com.meession.etm.module.crm.service.activity;

import com.meession.etm.module.crm.dal.dataobject.activity.CrmTaskDO;
import com.meession.etm.module.system.api.notify.NotifyMessageSendApi;
import com.meession.etm.module.system.api.notify.dto.NotifySendSingleToUserReqDTO;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
public class CrmActivityNotificationServiceImpl implements CrmActivityNotificationService {
    static final String TEMPLATE_ASSIGNED = "crm-task-assigned";
    static final String TEMPLATE_FINISHED = "crm-task-finished";

    @Resource
    private NotifyMessageSendApi notifyMessageSendApi;

    @Override
    public void notifyAssigned(CrmTaskDO task) {
        if (!Boolean.TRUE.equals(task.getNotifySystem())) return;
        Map<String, Object> params = new HashMap<>(2);
        params.put("title", task.getTitle());
        params.put("dueTime", task.getDueTime());
        send(task.getAssigneeUserId(), TEMPLATE_ASSIGNED, params);
    }

    @Override
    public void notifyFinished(CrmTaskDO task) {
        Long creatorUserId = parseUserId(task.getCreator());
        if (creatorUserId == null) return;
        Map<String, Object> params = new HashMap<>(2);
        params.put("title", task.getTitle());
        params.put("result", task.getResult());
        send(creatorUserId, TEMPLATE_FINISHED, params);
    }

    private void send(Long userId, String templateCode, Map<String, Object> params) {
        notifyMessageSendApi.sendSingleMessageToAdmin(new NotifySendSingleToUserReqDTO()
                .setUserId(userId).setTemplateCode(templateCode).setTemplateParams(params));
    }

    private static Long parseUserId(String value) {
        try {
            return value == null ? null : Long.valueOf(value);
        } catch (NumberFormatException ignored) {
            return null;
        }
    }
}
