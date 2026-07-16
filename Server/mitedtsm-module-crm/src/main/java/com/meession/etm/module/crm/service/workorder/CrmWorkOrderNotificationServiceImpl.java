package com.meession.etm.module.crm.service.workorder;

import com.meession.etm.module.crm.dal.dataobject.workorder.CrmWorkOrderDO;
import com.meession.etm.module.system.api.notify.NotifyMessageSendApi;
import com.meession.etm.module.system.api.notify.dto.NotifySendSingleToUserReqDTO;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Map;

@Service
public class CrmWorkOrderNotificationServiceImpl implements CrmWorkOrderNotificationService {

    public static final String TEMPLATE_ASSIGNED = "crm-work-order-assigned";
    public static final String TEMPLATE_RETURNED = "crm-work-order-returned";
    public static final String TEMPLATE_COMPLETED = "crm-work-order-completed";
    public static final String TEMPLATE_COPIED = "crm-work-order-copied";

    @Resource
    private NotifyMessageSendApi notifyMessageSendApi;

    @Override
    public void notifyAssigned(CrmWorkOrderDO workOrder) {
        if (workOrder.getHandlerUserId() != null) send(workOrder.getHandlerUserId(), TEMPLATE_ASSIGNED, workOrder);
    }

    @Override
    public void notifyCopied(CrmWorkOrderDO workOrder, Collection<Long> userIds) {
        if (userIds == null) return;
        for (Long userId : new LinkedHashSet<>(userIds)) {
            if (userId != null && !userId.equals(workOrder.getHandlerUserId())) {
                send(userId, TEMPLATE_COPIED, workOrder);
            }
        }
    }

    @Override
    public void notifyReturned(CrmWorkOrderDO workOrder) {
        send(Long.valueOf(workOrder.getCreator()), TEMPLATE_RETURNED, workOrder);
    }

    @Override
    public void notifyCompleted(CrmWorkOrderDO workOrder, Collection<Long> ccUserIds) {
        LinkedHashSet<Long> recipients = new LinkedHashSet<>();
        recipients.add(Long.valueOf(workOrder.getCreator()));
        if (ccUserIds != null) recipients.addAll(ccUserIds);
        recipients.forEach(userId -> send(userId, TEMPLATE_COMPLETED, workOrder));
    }

    private void send(Long userId, String templateCode, CrmWorkOrderDO workOrder) {
        Map<String, Object> params = new HashMap<>(3);
        params.put("no", workOrder.getNo());
        params.put("title", workOrder.getTitle());
        params.put("reason", workOrder.getReturnReason());
        notifyMessageSendApi.sendSingleMessageToAdmin(new NotifySendSingleToUserReqDTO()
                .setUserId(userId).setTemplateCode(templateCode).setTemplateParams(params));
    }
}
