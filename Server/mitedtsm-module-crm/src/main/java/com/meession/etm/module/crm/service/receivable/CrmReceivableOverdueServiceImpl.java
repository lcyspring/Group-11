package com.meession.etm.module.crm.service.receivable;

import cn.hutool.core.util.StrUtil;
import com.meession.etm.module.crm.dal.dataobject.receivable.CrmReceivableOverdueReminderDO;
import com.meession.etm.module.crm.dal.dataobject.receivable.CrmReceivablePlanDO;
import com.meession.etm.module.crm.dal.mysql.receivable.CrmReceivableOverdueReminderMapper;
import com.meession.etm.module.crm.dal.mysql.receivable.CrmReceivablePlanMapper;
import com.meession.etm.module.crm.framework.activity.CrmActivityProperties;
import com.meession.etm.module.system.api.notify.NotifyMessageSendApi;
import com.meession.etm.module.system.api.notify.dto.NotifySendSingleToUserReqDTO;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Map;

@Service
@Slf4j
public class CrmReceivableOverdueServiceImpl implements CrmReceivableOverdueService {
    static final String TEMPLATE_CODE = "crm-receivable-overdue";

    @Resource private CrmReceivableOverdueReminderMapper reminderMapper;
    @Resource private CrmReceivablePlanMapper planMapper;
    @Resource private NotifyMessageSendApi notifyMessageSendApi;
    @Resource private CrmActivityProperties properties;

    @Override
    public int scanAndNotify() {
        CrmActivityProperties.ReceivableOverdue policy = properties.getReceivableOverdue();
        ZoneId zone = ZoneId.of(policy.getZone());
        LocalDate reminderDate = LocalDate.now(zone);
        LocalDateTime todayStart = reminderDate.atStartOfDay();
        int sent = 0;
        for (int batch = 0; batch < policy.getMaxBatches(); batch++) {
            reminderMapper.createDueFacts(reminderDate, todayStart, policy.getBatchSize());
            var facts = reminderMapper.selectRetryable(policy.getMaxRetries(), policy.getBatchSize());
            if (facts.isEmpty()) break;
            for (CrmReceivableOverdueReminderDO fact : facts) {
                if (send(fact, todayStart)) sent++;
            }
            if (facts.size() < policy.getBatchSize()) break;
        }
        return sent;
    }

    private boolean send(CrmReceivableOverdueReminderDO fact, LocalDateTime todayStart) {
        try {
            CrmReceivablePlanDO plan = planMapper.selectById(fact.getReceivablePlanId());
            if (plan == null || reminderMapper.countStillOverdue(fact.getReceivablePlanId(), todayStart) == 0) {
                reminderMapper.markSent(fact.getId(), fact.getAttempts());
                return false;
            }
            notifyMessageSendApi.sendSingleMessageToAdmin(new NotifySendSingleToUserReqDTO()
                    .setUserId(fact.getRecipientUserId())
                    .setTemplateCode(TEMPLATE_CODE)
                    .setTemplateParams(Map.of(
                            "planId", plan.getId(),
                            "period", plan.getPeriod(),
                            "returnTime", plan.getReturnTime(),
                            "price", plan.getPrice())));
            if (reminderMapper.markSent(fact.getId(), fact.getAttempts()) == 1) return true;
            log.warn("[send][receivable overdue reminder {} sent but state changed concurrently]", fact.getId());
            return false;
        } catch (RuntimeException ex) {
            String error = StrUtil.maxLength(ex.getMessage() == null ? ex.getClass().getSimpleName() : ex.getMessage(), 1000);
            reminderMapper.markFailed(fact.getId(), fact.getAttempts(), error);
            log.warn("[send][receivable overdue reminder {} failed]", fact.getId(), ex);
            return false;
        }
    }
}
