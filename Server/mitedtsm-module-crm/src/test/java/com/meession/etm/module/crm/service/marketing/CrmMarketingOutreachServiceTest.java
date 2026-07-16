package com.meession.etm.module.crm.service.marketing;

import com.baomidou.mybatisplus.core.toolkit.support.SFunction;
import com.meession.etm.module.crm.dal.dataobject.marketing.CrmMarketingBroadcastDO;
import com.meession.etm.module.crm.dal.dataobject.marketing.CrmMarketingBroadcastRecipientDO;
import com.meession.etm.module.crm.dal.dataobject.marketing.CrmMarketingConsentDO;
import com.meession.etm.module.crm.dal.mysql.marketing.CrmMarketingBroadcastMapper;
import com.meession.etm.module.crm.dal.mysql.marketing.CrmMarketingBroadcastRecipientMapper;
import com.meession.etm.module.crm.dal.mysql.marketing.CrmMarketingConsentMapper;
import com.meession.etm.module.crm.enums.marketing.CrmMarketingBroadcastStatusEnum;
import com.meession.etm.module.crm.enums.marketing.CrmMarketingConsentStatusEnum;
import com.meession.etm.module.crm.enums.marketing.CrmMarketingRecipientStatusEnum;
import com.meession.etm.module.crm.framework.marketing.CrmMarketingProperties;
import com.meession.etm.module.system.api.sms.SmsSendApi;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CrmMarketingOutreachServiceTest {
    @Mock CrmMarketingBroadcastMapper broadcastMapper;
    @Mock CrmMarketingBroadcastRecipientMapper recipientMapper;
    @Mock CrmMarketingConsentMapper consentMapper;
    @Mock SmsSendApi smsSendApi;

    private CrmMarketingOutreachService service;
    private CrmMarketingProperties properties;

    @BeforeEach
    void setUp() {
        service = new CrmMarketingOutreachService();
        properties = new CrmMarketingProperties();
        properties.setBatchSize(2);
        properties.setMaxBatchSize(500);
        properties.setMonthlyRecipientLimit(10000);
        ReflectionTestUtils.setField(service, "broadcastMapper", broadcastMapper);
        ReflectionTestUtils.setField(service, "recipientMapper", recipientMapper);
        ReflectionTestUtils.setField(service, "consentMapper", consentMapper);
        ReflectionTestUtils.setField(service, "smsSendApi", smsSendApi);
        ReflectionTestUtils.setField(service, "properties", properties);
    }

    @Test
    void contactUsesCustomerConsentOnlyWhenContactRecordIsMissing() {
        CrmMarketingConsentDO customerConsent = new CrmMarketingConsentDO()
                .setStatus(CrmMarketingConsentStatusEnum.OPTED_IN.getStatus());
        when(consentMapper.selectTarget(12L, 21L, 1)).thenReturn(null);
        when(consentMapper.selectTarget(12L, null, 1)).thenReturn(customerConsent);

        assertSame(customerConsent, service.resolveConsent(12L, 21L, 1));
    }

    @Test
    void contactOptOutOverridesCustomerConsent() {
        CrmMarketingConsentDO contactOptOut = new CrmMarketingConsentDO()
                .setStatus(CrmMarketingConsentStatusEnum.OPTED_OUT.getStatus());
        when(consentMapper.selectTarget(12L, 21L, 1)).thenReturn(contactOptOut);

        assertSame(contactOptOut, service.resolveConsent(12L, 21L, 1));
        verify(consentMapper, never()).selectTarget(12L, null, 1);
    }

    @Test
    void recordOnlyModeProcessesAllConfiguredBatchesBeforeCompleting() {
        properties.setProviderMode("record-only");
        CrmMarketingBroadcastDO broadcast = new CrmMarketingBroadcastDO().setId(8L);
        CrmMarketingBroadcastRecipientDO first = pending(1L, 8L);
        CrmMarketingBroadcastRecipientDO second = pending(2L, 8L);
        CrmMarketingBroadcastRecipientDO third = pending(3L, 8L);
        when(recipientMapper.selectPending(8L, 2))
                .thenReturn(List.of(first, second), List.of(third), List.of());
        when(recipientMapper.claimForSending(any(), any())).thenReturn(1);
        when(recipientMapper.selectList(any(SFunction.class), eq(8L)))
                .thenReturn(List.of(first, second, third));

        service.sendRecipients(broadcast);

        assertEquals(CrmMarketingBroadcastStatusEnum.SENT.getStatus(), broadcast.getStatus());
        assertEquals(3, broadcast.getSentCount());
        verify(recipientMapper, times(3)).updateById(any(CrmMarketingBroadcastRecipientDO.class));
        verify(recipientMapper, times(3)).selectPending(8L, 2);
    }

    @Test
    void providerFailureStopsCurrentAttemptAndMarksPartialFailure() {
        properties.setProviderMode("system");
        CrmMarketingBroadcastDO broadcast = new CrmMarketingBroadcastDO().setId(9L).setSmsTemplateCode("crm-sms");
        CrmMarketingBroadcastRecipientDO recipient = pending(4L, 9L).setMobile("13800000000");
        when(recipientMapper.selectPending(9L, 2)).thenReturn(List.of(recipient), List.of());
        when(recipientMapper.claimForSending(any(), any())).thenReturn(1);
        when(smsSendApi.sendSingleSmsToAdmin(any())).thenThrow(new IllegalStateException("provider unavailable"));
        when(recipientMapper.selectList(any(SFunction.class), eq(9L))).thenReturn(List.of(recipient));

        service.sendRecipients(broadcast);

        assertEquals(CrmMarketingRecipientStatusEnum.FAILED.getStatus(), recipient.getStatus());
        assertEquals(CrmMarketingBroadcastStatusEnum.PARTIAL_FAILED.getStatus(), broadcast.getStatus());
        verify(smsSendApi, times(1)).sendSingleSmsToAdmin(any());
    }

    @Test
    void retryExplicitlyResetsFailedRecipients() {
        properties.setProviderMode("record-only");
        CrmMarketingBroadcastDO broadcast = new CrmMarketingBroadcastDO().setId(10L)
                .setStatus(CrmMarketingBroadcastStatusEnum.PARTIAL_FAILED.getStatus());
        when(broadcastMapper.selectById(10L)).thenReturn(broadcast);
        when(recipientMapper.resetFailed(10L)).thenReturn(2);
        when(recipientMapper.selectCount(any())).thenReturn(0L);
        when(recipientMapper.selectPending(eq(10L), anyInt())).thenReturn(List.of());
        when(recipientMapper.selectList(any(SFunction.class), eq(10L))).thenReturn(List.of());

        service.retryFailed(10L);

        verify(recipientMapper).resetFailed(10L);
    }

    private static CrmMarketingBroadcastRecipientDO pending(Long id, Long broadcastId) {
        return new CrmMarketingBroadcastRecipientDO().setId(id).setBroadcastId(broadcastId).setChannel(1)
                .setStatus(CrmMarketingRecipientStatusEnum.PENDING.getStatus()).setAttemptCount(0);
    }
}
