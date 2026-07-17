package com.meession.etm.module.crm.service.marketing;

import com.baomidou.mybatisplus.core.toolkit.support.SFunction;
import com.meession.etm.module.crm.dal.dataobject.marketing.CrmMarketingBroadcastDO;
import com.meession.etm.module.crm.dal.dataobject.marketing.CrmMarketingBroadcastRecipientDO;
import com.meession.etm.module.crm.dal.dataobject.marketing.CrmMarketingConsentDO;
import com.meession.etm.module.crm.dal.mysql.marketing.CrmMarketingBroadcastMapper;
import com.meession.etm.module.crm.dal.mysql.marketing.CrmMarketingBroadcastRecipientMapper;
import com.meession.etm.module.crm.dal.mysql.marketing.CrmMarketingConsentMapper;
import com.meession.etm.module.crm.dal.mysql.customer.CrmCustomerMapper;
import com.meession.etm.module.crm.dal.mysql.contact.CrmContactMapper;
import com.meession.etm.module.crm.dal.dataobject.customer.CrmCustomerDO;
import com.meession.etm.module.crm.dal.dataobject.contact.CrmContactDO;
import com.meession.etm.module.crm.enums.marketing.CrmMarketingBroadcastStatusEnum;
import com.meession.etm.module.crm.enums.marketing.CrmMarketingConsentStatusEnum;
import com.meession.etm.module.crm.enums.marketing.CrmMarketingRecipientStatusEnum;
import com.meession.etm.module.crm.enums.marketing.CrmMarketingDeliveryStatusEnum;
import com.meession.etm.module.crm.framework.marketing.CrmMarketingProperties;
import com.meession.etm.module.crm.framework.permission.CrmAuthorizationService;
import com.meession.etm.module.crm.framework.permission.CrmOwnerReadScope;
import com.meession.etm.module.crm.controller.admin.marketing.vo.CrmMarketingReviewReqVO;
import com.meession.etm.module.crm.controller.admin.marketing.vo.CrmMarketingBroadcastPageReqVO;
import com.meession.etm.framework.common.exception.ServiceException;
import com.meession.etm.module.system.api.sms.SmsSendApi;
import com.meession.etm.module.system.api.sms.dto.SmsSendStatusRespDTO;
import com.meession.etm.module.system.api.mail.MailSendApi;
import com.meession.etm.module.system.api.mail.dto.MailSendStatusRespDTO;
import com.meession.etm.module.system.enums.sms.SmsSendStatusEnum;
import com.meession.etm.module.system.enums.sms.SmsReceiveStatusEnum;
import com.meession.etm.module.system.enums.mail.MailSendStatusEnum;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.time.LocalDateTime;
import org.mockito.ArgumentCaptor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CrmMarketingOutreachServiceTest {
    @Mock CrmMarketingBroadcastMapper broadcastMapper;
    @Mock CrmMarketingBroadcastRecipientMapper recipientMapper;
    @Mock CrmMarketingConsentMapper consentMapper;
    @Mock SmsSendApi smsSendApi;
    @Mock MailSendApi mailSendApi;
    @Mock CrmAuthorizationService authorizationService;
    @Mock CrmCustomerMapper customerMapper;
    @Mock CrmContactMapper contactMapper;

    private CrmMarketingOutreachService service;
    private CrmMarketingProperties properties;

    @BeforeEach
    void setUp() {
        service = new CrmMarketingOutreachService();
        properties = new CrmMarketingProperties();
        properties.setBatchSize(2);
        properties.setMaxBatchSize(500);
        properties.setMonthlyRecipientLimit(10000);
        properties.setPublicBaseUrl("https://crm.example.com");
        properties.setDeliverySyncBatchSize(200);
        ReflectionTestUtils.setField(service, "broadcastMapper", broadcastMapper);
        ReflectionTestUtils.setField(service, "recipientMapper", recipientMapper);
        ReflectionTestUtils.setField(service, "consentMapper", consentMapper);
        ReflectionTestUtils.setField(service, "smsSendApi", smsSendApi);
        ReflectionTestUtils.setField(service, "mailSendApi", mailSendApi);
        ReflectionTestUtils.setField(service, "authorizationService", authorizationService);
        ReflectionTestUtils.setField(service, "customerMapper", customerMapper);
        ReflectionTestUtils.setField(service, "contactMapper", contactMapper);
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
        when(broadcastMapper.transition(eq(10L), any(),
                eq(CrmMarketingBroadcastStatusEnum.SENDING.getStatus()))).thenReturn(1);
        when(recipientMapper.selectCount(any())).thenReturn(0L);
        when(recipientMapper.selectPending(eq(10L), anyInt())).thenReturn(List.of());
        when(recipientMapper.selectList(any(SFunction.class), eq(10L))).thenReturn(List.of());

        service.retryFailed(10L);

        verify(recipientMapper).resetFailed(10L);
    }

    @Test
    void creatorCanDeleteDraftAndRecipientsTogether() {
        CrmMarketingBroadcastDO broadcast = new CrmMarketingBroadcastDO().setId(11L)
                .setStatus(CrmMarketingBroadcastStatusEnum.DRAFT.getStatus());
        broadcast.setCreator("7");
        when(broadcastMapper.selectById(11L)).thenReturn(broadcast);
        when(broadcastMapper.deleteDraft(11L, CrmMarketingBroadcastStatusEnum.DRAFT.getStatus())).thenReturn(1);

        service.deleteBroadcast(11L, 7L);

        verify(broadcastMapper).deleteDraft(11L, CrmMarketingBroadcastStatusEnum.DRAFT.getStatus());
    }

    @Test
    void nonCreatorCannotDeleteAnotherUsersDraft() {
        CrmMarketingBroadcastDO broadcast = new CrmMarketingBroadcastDO().setId(12L)
                .setStatus(CrmMarketingBroadcastStatusEnum.DRAFT.getStatus());
        broadcast.setCreator("7");
        when(broadcastMapper.selectById(12L)).thenReturn(broadcast);
        when(authorizationService.isCrmAdmin(8L)).thenReturn(false);

        assertThrows(ServiceException.class, () -> service.deleteBroadcast(12L, 8L));
        verify(broadcastMapper, never()).deleteDraft(any(), any());
    }

    @Test
    void rejectedReviewRequiresAReason() {
        CrmMarketingBroadcastDO broadcast = new CrmMarketingBroadcastDO().setId(13L)
                .setStatus(CrmMarketingBroadcastStatusEnum.PENDING_REVIEW.getStatus());
        broadcast.setCreator("7");
        when(broadcastMapper.selectById(13L)).thenReturn(broadcast);

        assertThrows(ServiceException.class, () -> service.review(
                new CrmMarketingReviewReqVO().setId(13L).setComment("  "), 8L, false));
        verify(broadcastMapper, never()).reviewIfPending(any(), any(), any(), any(), any(), any());
    }

    @Test
    void submitReviewUsesAtomicStatusTransition() {
        CrmMarketingBroadcastDO broadcast = new CrmMarketingBroadcastDO().setId(14L)
                .setStatus(CrmMarketingBroadcastStatusEnum.DRAFT.getStatus());
        broadcast.setCreator("7");
        when(broadcastMapper.selectById(14L)).thenReturn(broadcast);
        when(broadcastMapper.transition(eq(14L), any(),
                eq(CrmMarketingBroadcastStatusEnum.PENDING_REVIEW.getStatus()))).thenReturn(0);

        assertThrows(ServiceException.class, () -> service.submitReview(14L, 7L));
    }

    @Test
    void targetOptionsFollowOwnerReadScopeWithoutContactQueryPermission() {
        CrmCustomerDO own = new CrmCustomerDO().setId(21L).setOwnerUserId(7L);
        when(customerMapper.selectList(any(com.meession.etm.framework.mybatis.core.query.LambdaQueryWrapperX.class)))
                .thenReturn(List.of(own));
        when(authorizationService.resolveOwnerReadScope(7L))
                .thenReturn(new CrmOwnerReadScope(false, java.util.Set.of(7L)));
        CrmContactDO ownContact = new CrmContactDO().setId(31L).setCustomerId(21L);
        when(contactMapper.selectList(any(com.meession.etm.framework.mybatis.core.query.LambdaQueryWrapperX.class)))
                .thenReturn(List.of(ownContact));

        List<CrmCustomerDO> customers = service.getTargetCustomers(7L);
        List<CrmContactDO> contacts = service.getTargetContacts(customers);

        assertEquals(List.of(own), customers);
        assertEquals(List.of(ownContact), contacts);
    }

    @Test
    void queryOnlyUserCannotReadAnotherCreatorsBroadcastOrRecipients() {
        CrmMarketingBroadcastDO broadcast = new CrmMarketingBroadcastDO().setId(41L);
        broadcast.setCreator("7");
        when(broadcastMapper.selectById(41L)).thenReturn(broadcast);
        when(authorizationService.isCrmAdmin(8L)).thenReturn(false);

        assertThrows(ServiceException.class, () -> service.getBroadcast(41L, 8L, false));
        assertThrows(ServiceException.class, () -> service.getBroadcastRecipients(41L, 8L, false));
        verify(recipientMapper, never()).selectList(any(SFunction.class), any());
    }

    @Test
    void creatorAndPrivilegedReviewerCanReadBroadcastRecipients() {
        CrmMarketingBroadcastDO broadcast = new CrmMarketingBroadcastDO().setId(42L);
        broadcast.setCreator("7");
        when(broadcastMapper.selectById(42L)).thenReturn(broadcast);
        when(recipientMapper.selectList(any(SFunction.class), eq(42L))).thenReturn(List.of());

        assertSame(broadcast, service.getBroadcast(42L, 7L, false));
        service.getBroadcastRecipients(42L, 8L, true);

        verify(recipientMapper).selectList(any(SFunction.class), eq(42L));
    }

    @Test
    void queryOnlyPageIsCreatorScopedWhileReviewerCanReadAll() {
        CrmMarketingBroadcastPageReqVO request = new CrmMarketingBroadcastPageReqVO();
        when(authorizationService.isCrmAdmin(8L)).thenReturn(false);

        service.getBroadcastPage(request, 8L, false);
        service.getBroadcastPage(request, 8L, true);

        verify(broadcastMapper).selectPage(request, false, "8");
        verify(broadcastMapper).selectPage(request, true, "8");
    }

    @Test
    void smsDeliveryReceiptBecomesDeliveredWithProviderTime() {
        CrmMarketingBroadcastDO broadcast = readableBroadcast(51L, 7L);
        LocalDateTime receivedAt = LocalDateTime.of(2026, 7, 17, 9, 30);
        CrmMarketingBroadcastRecipientDO recipient = providerPending(1L, 51L, 1, 101L);
        when(broadcastMapper.selectById(51L)).thenReturn(broadcast);
        when(recipientMapper.selectList(any(SFunction.class), eq(51L))).thenReturn(List.of(recipient));
        when(smsSendApi.getSmsSendStatus(101L)).thenReturn(new SmsSendStatusRespDTO()
                .setSendStatus(SmsSendStatusEnum.SUCCESS.getStatus())
                .setReceiveStatus(SmsReceiveStatusEnum.SUCCESS.getStatus()).setReceiveTime(receivedAt));

        assertEquals(1, service.syncDeliveryResults(51L, 7L, false));

        ArgumentCaptor<CrmMarketingBroadcastRecipientDO> update =
                ArgumentCaptor.forClass(CrmMarketingBroadcastRecipientDO.class);
        verify(recipientMapper).updateById(update.capture());
        assertEquals(CrmMarketingDeliveryStatusEnum.DELIVERED.getStatus(), update.getValue().getDeliveryStatus());
        assertEquals(receivedAt, update.getValue().getDeliveredAt());
    }

    @Test
    void smsFailedReceiptPreservesProviderReason() {
        CrmMarketingBroadcastDO broadcast = readableBroadcast(52L, 7L);
        CrmMarketingBroadcastRecipientDO recipient = providerPending(2L, 52L, 1, 102L);
        when(broadcastMapper.selectById(52L)).thenReturn(broadcast);
        when(recipientMapper.selectList(any(SFunction.class), eq(52L))).thenReturn(List.of(recipient));
        when(smsSendApi.getSmsSendStatus(102L)).thenReturn(new SmsSendStatusRespDTO()
                .setSendStatus(SmsSendStatusEnum.SUCCESS.getStatus())
                .setReceiveStatus(SmsReceiveStatusEnum.FAILURE.getStatus()).setReceiveMessage("carrier rejected"));

        service.syncDeliveryResults(52L, 7L, false);

        ArgumentCaptor<CrmMarketingBroadcastRecipientDO> update =
                ArgumentCaptor.forClass(CrmMarketingBroadcastRecipientDO.class);
        verify(recipientMapper).updateById(update.capture());
        assertEquals(CrmMarketingDeliveryStatusEnum.FAILED.getStatus(), update.getValue().getDeliveryStatus());
        assertEquals("carrier rejected", update.getValue().getFailureReason());
    }

    @Test
    void successfulMailMeansAcceptedNotDelivered() {
        CrmMarketingBroadcastDO broadcast = readableBroadcast(53L, 7L);
        LocalDateTime acceptedAt = LocalDateTime.of(2026, 7, 17, 9, 31);
        CrmMarketingBroadcastRecipientDO recipient = providerPending(3L, 53L, 2, 103L);
        when(broadcastMapper.selectById(53L)).thenReturn(broadcast);
        when(recipientMapper.selectList(any(SFunction.class), eq(53L))).thenReturn(List.of(recipient));
        when(mailSendApi.getMailSendStatus(103L)).thenReturn(new MailSendStatusRespDTO()
                .setSendStatus(MailSendStatusEnum.SUCCESS.getStatus()).setSendTime(acceptedAt));

        service.syncDeliveryResults(53L, 7L, false);

        ArgumentCaptor<CrmMarketingBroadcastRecipientDO> update =
                ArgumentCaptor.forClass(CrmMarketingBroadcastRecipientDO.class);
        verify(recipientMapper).updateById(update.capture());
        assertEquals(CrmMarketingDeliveryStatusEnum.ACCEPTED.getStatus(), update.getValue().getDeliveryStatus());
        assertEquals(acceptedAt, update.getValue().getDeliveredAt());
    }

    @Test
    void trackingTokenIsChannelBoundIdempotentAndOpaqueForInvalidValues() {
        CrmMarketingBroadcastRecipientDO email = new CrmMarketingBroadcastRecipientDO().setChannel(2);
        when(recipientMapper.selectByTrackingToken("0123456789abcdef0123456789abcdef")).thenReturn(email);

        service.recordMailOpen("0123456789abcdef0123456789abcdef");
        service.recordMailOpen("bad-token");

        verify(recipientMapper).markOpened(eq("0123456789abcdef0123456789abcdef"), any(LocalDateTime.class));
        verify(recipientMapper, never()).selectByTrackingToken("bad-token");
    }

    @Test
    void deliverySummarySeparatesSmsDeliveryAndEmailOpeningRates() {
        CrmMarketingBroadcastDO broadcast = readableBroadcast(54L, 7L);
        CrmMarketingBroadcastRecipientDO smsDelivered = providerPending(4L, 54L, 1, 104L)
                .setDeliveryStatus(CrmMarketingDeliveryStatusEnum.DELIVERED.getStatus());
        CrmMarketingBroadcastRecipientDO smsPending = providerPending(5L, 54L, 1, 105L);
        CrmMarketingBroadcastRecipientDO mailOpened = providerPending(6L, 54L, 2, 106L)
                .setDeliveryStatus(CrmMarketingDeliveryStatusEnum.ACCEPTED.getStatus())
                .setOpenedAt(LocalDateTime.now());
        when(broadcastMapper.selectById(54L)).thenReturn(broadcast);
        when(recipientMapper.selectList(any(SFunction.class), eq(54L)))
                .thenReturn(List.of(smsDelivered, smsPending, mailOpened));

        var summary = service.getDeliverySummary(54L, 7L, false);

        assertEquals(2, summary.getSmsSentCount());
        assertEquals("50.00", summary.getSmsDeliveryRate().toPlainString());
        assertEquals(1, summary.getEmailAcceptedCount());
        assertEquals(1, summary.getEmailOpenedCount());
        assertEquals("100.00", summary.getEmailOpenRate().toPlainString());
    }

    private static CrmMarketingBroadcastRecipientDO pending(Long id, Long broadcastId) {
        return new CrmMarketingBroadcastRecipientDO().setId(id).setBroadcastId(broadcastId).setChannel(1)
                .setStatus(CrmMarketingRecipientStatusEnum.PENDING.getStatus()).setAttemptCount(0);
    }

    private static CrmMarketingBroadcastRecipientDO providerPending(Long id, Long broadcastId,
                                                                     int channel, Long logId) {
        return new CrmMarketingBroadcastRecipientDO().setId(id).setBroadcastId(broadcastId).setChannel(channel)
                .setProviderLogId(logId)
                .setDeliveryStatus(CrmMarketingDeliveryStatusEnum.PROVIDER_PENDING.getStatus());
    }

    private static CrmMarketingBroadcastDO readableBroadcast(Long id, Long creatorUserId) {
        CrmMarketingBroadcastDO broadcast = new CrmMarketingBroadcastDO().setId(id);
        broadcast.setCreator(String.valueOf(creatorUserId));
        return broadcast;
    }
}
