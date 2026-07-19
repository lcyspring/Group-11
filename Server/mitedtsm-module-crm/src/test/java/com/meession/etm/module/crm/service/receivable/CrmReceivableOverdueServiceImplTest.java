package com.meession.etm.module.crm.service.receivable;

import com.meession.etm.module.crm.dal.dataobject.receivable.CrmReceivableOverdueReminderDO;
import com.meession.etm.module.crm.dal.dataobject.receivable.CrmReceivablePlanDO;
import com.meession.etm.module.crm.dal.mysql.receivable.CrmReceivableOverdueReminderMapper;
import com.meession.etm.module.crm.dal.mysql.receivable.CrmReceivablePlanMapper;
import com.meession.etm.module.crm.framework.activity.CrmActivityProperties;
import com.meession.etm.module.system.api.notify.NotifyMessageSendApi;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CrmReceivableOverdueServiceImplTest {
    @Mock private CrmReceivableOverdueReminderMapper reminderMapper;
    @Mock private CrmReceivablePlanMapper planMapper;
    @Mock private NotifyMessageSendApi notifyMessageSendApi;
    @Mock private CrmActivityProperties properties;
    @InjectMocks private CrmReceivableOverdueServiceImpl service;

    @BeforeEach
    void policy() {
        CrmActivityProperties.ReceivableOverdue policy = new CrmActivityProperties.ReceivableOverdue();
        policy.setZone("Asia/Shanghai");
        policy.setBatchSize(20);
        policy.setMaxBatches(2);
        policy.setMaxRetries(5);
        when(properties.getReceivableOverdue()).thenReturn(policy);
    }

    @Test
    void createsDailyFactAndSendsIdempotentNotification() {
        CrmReceivableOverdueReminderDO fact = new CrmReceivableOverdueReminderDO()
                .setId(9L).setReceivablePlanId(3L).setRecipientUserId(8L).setAttempts(0);
        when(reminderMapper.selectRetryable(5, 20)).thenReturn(List.of(fact), List.of());
        when(planMapper.selectById(3L)).thenReturn(new CrmReceivablePlanDO().setId(3L).setOwnerUserId(8L)
                .setPeriod(2).setReturnTime(LocalDateTime.of(2026, 7, 1, 0, 0)).setPrice(new BigDecimal("100.00")));
        when(reminderMapper.countStillOverdue(eq(3L), any())).thenReturn(1);
        when(reminderMapper.markSent(9L, 0)).thenReturn(1);

        assertEquals(1, service.scanAndNotify());
        verify(reminderMapper).createDueFacts(any(), any(), eq(20));
        verify(notifyMessageSendApi).sendSingleMessageToAdmin(argThat(request ->
                request.getUserId().equals(8L) && request.getTemplateCode().equals("crm-receivable-overdue")
                        && request.getTemplateParams().get("planId").equals(3L)));
    }

    @Test
    void failedDeliveryIsPersistedForBoundedRetry() {
        CrmReceivableOverdueReminderDO fact = new CrmReceivableOverdueReminderDO()
                .setId(9L).setReceivablePlanId(3L).setRecipientUserId(8L).setAttempts(1);
        when(reminderMapper.selectRetryable(5, 20)).thenReturn(List.of(fact), List.of());
        when(planMapper.selectById(3L)).thenReturn(new CrmReceivablePlanDO().setId(3L).setPeriod(2)
                .setReturnTime(LocalDateTime.now()).setPrice(BigDecimal.TEN));
        when(reminderMapper.countStillOverdue(eq(3L), any())).thenReturn(1);
        doThrow(new IllegalStateException("provider unavailable"))
                .when(notifyMessageSendApi).sendSingleMessageToAdmin(any());

        assertEquals(0, service.scanAndNotify());
        verify(reminderMapper).markFailed(eq(9L), eq(1), contains("provider unavailable"));
    }
}
