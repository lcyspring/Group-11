package com.meession.etm.module.crm.service.marketing;

import com.meession.etm.framework.common.exception.ServiceException;
import com.meession.etm.framework.common.pojo.PageResult;
import com.meession.etm.module.crm.controller.admin.marketing.vo.*;
import com.meession.etm.module.crm.dal.dataobject.contact.CrmContactDO;
import com.meession.etm.module.crm.dal.dataobject.customer.CrmCustomerDO;
import com.meession.etm.module.crm.dal.dataobject.marketing.CrmCustomerCarePlanDO;
import com.meession.etm.module.crm.dal.dataobject.marketing.CrmCustomerCareRecordDO;
import com.meession.etm.module.crm.dal.dataobject.marketing.CrmMarketingConsentDO;
import com.meession.etm.module.crm.dal.mysql.contact.CrmContactMapper;
import com.meession.etm.module.crm.dal.mysql.customer.CrmCustomerMapper;
import com.meession.etm.module.crm.dal.mysql.marketing.*;
import com.meession.etm.module.crm.enums.marketing.CrmMarketingConsentStatusEnum;
import com.meession.etm.module.crm.enums.marketing.CrmMarketingRecipientStatusEnum;
import com.meession.etm.module.crm.framework.marketing.CrmMarketingProperties;
import com.meession.etm.module.crm.framework.permission.CrmAuthorizationService;
import com.meession.etm.module.crm.framework.permission.CrmOwnerReadScope;
import com.meession.etm.module.system.api.mail.MailSendApi;
import com.meession.etm.module.system.api.sms.SmsSendApi;
import com.meession.etm.module.system.api.sms.dto.send.SmsSendSingleToUserReqDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

import static com.meession.etm.module.crm.enums.ErrorCodeConstants.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CrmCustomerCareServiceImplTest {
    @Mock CrmCustomerCarePlanMapper planMapper;
    @Mock CrmCustomerCareRecordMapper recordMapper;
    @Mock CrmMarketingBroadcastRecipientMapper broadcastRecipientMapper;
    @Mock CrmMarketingConsentMapper consentMapper;
    @Mock CrmCustomerMapper customerMapper;
    @Mock CrmContactMapper contactMapper;
    @Mock CrmAuthorizationService authorizationService;
    @Mock SmsSendApi smsSendApi;
    @Mock MailSendApi mailSendApi;

    private CrmCustomerCareServiceImpl service;
    private CrmMarketingProperties properties;

    @BeforeEach
    void setUp() {
        service = new CrmCustomerCareServiceImpl();
        properties = new CrmMarketingProperties();
        properties.setProviderMode("record-only");
        properties.setCareZone("Asia/Shanghai");
        ReflectionTestUtils.setField(service, "planMapper", planMapper);
        ReflectionTestUtils.setField(service, "recordMapper", recordMapper);
        ReflectionTestUtils.setField(service, "broadcastRecipientMapper", broadcastRecipientMapper);
        ReflectionTestUtils.setField(service, "consentMapper", consentMapper);
        ReflectionTestUtils.setField(service, "customerMapper", customerMapper);
        ReflectionTestUtils.setField(service, "contactMapper", contactMapper);
        ReflectionTestUtils.setField(service, "authorizationService", authorizationService);
        ReflectionTestUtils.setField(service, "properties", properties);
        ReflectionTestUtils.setField(service, "smsSendApi", smsSendApi);
        ReflectionTestUtils.setField(service, "mailSendApi", mailSendApi);
    }

    @Test
    void birthdayPlanNormalizesRuleFieldsAndDerivesScope() {
        doAnswer(invocation -> {
            invocation.<CrmCustomerCarePlanDO>getArgument(0).setId(9L);
            return 1;
        }).when(planMapper).insert(any(CrmCustomerCarePlanDO.class));

        Long id = service.savePlan(plan(1).setEventMonthDay("12-31").setFollowUpDays(7));

        assertEquals(9L, id);
        ArgumentCaptor<CrmCustomerCarePlanDO> captor = ArgumentCaptor.forClass(CrmCustomerCarePlanDO.class);
        verify(planMapper).insert(captor.capture());
        assertNull(captor.getValue().getEventMonthDay());
        assertNull(captor.getValue().getFollowUpDays());
        assertEquals("BIRTHDAY_CONTACTS", captor.getValue().getTargetScope());
    }

    @Test
    void holidayRejectsImpossibleCalendarDate() {
        ServiceException error = assertThrows(ServiceException.class,
                () -> service.savePlan(plan(2).setEventMonthDay("02-31")));
        assertEquals(MARKETING_CARE_RULE_INVALID.getCode(), error.getCode());
        verifyNoInteractions(planMapper);
    }

    @Test
    void postDealFollowUpRequiresBoundedDelay() {
        ServiceException error = assertThrows(ServiceException.class,
                () -> service.savePlan(plan(3).setFollowUpDays(0)));
        assertEquals(MARKETING_CARE_RULE_INVALID.getCode(), error.getCode());
    }

    @Test
    void enabledPlanCannotBeDeleted() {
        when(planMapper.selectById(5L)).thenReturn(new CrmCustomerCarePlanDO().setId(5L).setEnabled(true));
        ServiceException error = assertThrows(ServiceException.class, () -> service.deletePlan(5L));
        assertEquals(MARKETING_CARE_PLAN_ENABLED.getCode(), error.getCode());
        verify(planMapper, never()).deleteDisabled(anyLong());
    }

    @Test
    void planStatusUsesExplicitUpdate() {
        when(planMapper.selectById(6L)).thenReturn(new CrmCustomerCarePlanDO().setId(6L));
        when(planMapper.updateEnabled(6L, true)).thenReturn(1);
        service.updatePlanStatus(new CrmCustomerCarePlanStatusReqVO().setId(6L).setEnabled(true));
        verify(planMapper).updateEnabled(6L, true);
    }

    @Test
    void recordPageAppliesOwnerScopeAndResolvesReadableNames() {
        CrmCustomerCareRecordDO record = new CrmCustomerCareRecordDO().setId(1L).setPlanId(2L)
                .setCustomerId(3L).setContactId(4L);
        when(authorizationService.resolveOwnerReadScope(7L))
                .thenReturn(new CrmOwnerReadScope(false, Set.of(7L)));
        when(recordMapper.selectPage(any(), eq(false), eq(Set.of(7L))))
                .thenReturn(new PageResult<>(List.of(record), 1L));
        when(planMapper.selectByIds(anyCollection()))
                .thenReturn(List.of(new CrmCustomerCarePlanDO().setId(2L).setName("生日关怀")));
        when(customerMapper.selectByIds(anyCollection()))
                .thenReturn(List.of(new CrmCustomerDO().setId(3L).setName("示例客户")));
        when(contactMapper.selectByIds(anyCollection()))
                .thenReturn(List.of(new CrmContactDO().setId(4L).setName("联系人")));

        PageResult<CrmCustomerCareRecordRespVO> page = service.getRecordPage(
                new CrmCustomerCareRecordPageReqVO(), 7L);

        assertEquals("生日关怀", page.getList().get(0).getPlanName());
        assertEquals("示例客户", page.getList().get(0).getCustomerName());
        assertEquals("联系人", page.getList().get(0).getContactName());
    }

    @Test
    void birthdayGenerationCreatesIdempotentRecordedResult() {
        LocalDate today = LocalDate.now(java.time.ZoneId.of(properties.getCareZone()));
        CrmCustomerCarePlanDO plan = new CrmCustomerCarePlanDO().setId(11L).setRuleType(1)
                .setChannel(1).setSmsTemplateCode("birthday").setEnabled(true);
        CrmContactDO contact = new CrmContactDO().setId(12L).setCustomerId(13L)
                .setName("联系人").setMobile("13800000000");
        when(planMapper.selectEnabledByEventDay(anyString())).thenReturn(List.of(plan));
        when(contactMapper.selectBirthdayContacts(anyString())).thenReturn(List.of(contact));
        when(customerMapper.selectById(13L)).thenReturn(new CrmCustomerDO().setId(13L).setName("客户"));
        when(consentMapper.selectTarget(13L, 12L, 1)).thenReturn(new CrmMarketingConsentDO()
                .setStatus(CrmMarketingConsentStatusEnum.OPTED_IN.getStatus()));
        doAnswer(invocation -> {
            invocation.<CrmCustomerCareRecordDO>getArgument(0).setId(14L);
            return 1;
        }).when(recordMapper).insert(any(CrmCustomerCareRecordDO.class));

        assertEquals(1, service.generateAndSendToday());

        ArgumentCaptor<CrmCustomerCareRecordDO> captor = ArgumentCaptor.forClass(CrmCustomerCareRecordDO.class);
        verify(recordMapper).updateById(captor.capture());
        assertEquals(today, captor.getValue().getEventDate());
        assertEquals(CrmMarketingRecipientStatusEnum.RECORDED.getStatus(), captor.getValue().getStatus());
    }

    @Test
    void holidayTargetsOnlyPrimaryContactsOfDealCustomers() {
        CrmCustomerCarePlanDO plan = new CrmCustomerCarePlanDO().setId(21L).setRuleType(2)
                .setEventMonthDay("01-01").setChannel(2).setMailTemplateCode("holiday").setEnabled(true);
        when(planMapper.selectEnabledByEventDay(anyString())).thenReturn(List.of(plan));
        when(contactMapper.selectPrimaryContactsByLifecycleStatus(30)).thenReturn(List.of());

        assertEquals(0, service.generateAndSendToday());
        verify(contactMapper).selectPrimaryContactsByLifecycleStatus(30);
        verify(contactMapper, never()).selectBirthdayContacts(anyString());
    }

    @Test
    void postDealFollowUpUsesConfiguredLifecycleOffset() {
        CrmCustomerCarePlanDO plan = new CrmCustomerCarePlanDO().setId(31L).setRuleType(3)
                .setFollowUpDays(7).setChannel(1).setSmsTemplateCode("survey").setEnabled(true);
        when(planMapper.selectEnabledByEventDay(anyString())).thenReturn(List.of(plan));
        when(contactMapper.selectPrimaryContactsByLifecycleChangedBetween(eq(30), any(), any()))
                .thenReturn(List.of());

        service.generateAndSendToday();

        ArgumentCaptor<LocalDateTime> begin = ArgumentCaptor.forClass(LocalDateTime.class);
        verify(contactMapper).selectPrimaryContactsByLifecycleChangedBetween(eq(30), begin.capture(), any());
        assertEquals(LocalDate.now(java.time.ZoneId.of(properties.getCareZone())).minusDays(7),
                begin.getValue().toLocalDate());
    }

    @Test
    void leapDayBirthdayUsesFebruaryLastDayInNonLeapYear() {
        assertEquals(LocalDate.of(2027, 2, 28), CrmCustomerCareServiceImpl.nextBirthday(
                LocalDate.of(2000, 2, 29), LocalDate.of(2027, 2, 1)));
    }

    @Test
    void missingConsentCreatesSuppressedAuditRecord() {
        stubBirthdayTarget();
        ArgumentCaptor<CrmCustomerCareRecordDO> captor = ArgumentCaptor.forClass(CrmCustomerCareRecordDO.class);

        assertEquals(1, service.generateAndSendToday());

        verify(recordMapper).insert(captor.capture());
        assertEquals(CrmMarketingRecipientStatusEnum.SUPPRESSED.getStatus(), captor.getValue().getStatus());
        assertTrue(captor.getValue().getFailureReason().contains("同意"));
        verify(recordMapper, never()).updateById(any(CrmCustomerCareRecordDO.class));
    }

    @Test
    void systemProviderReceivesCustomerContactAndEventTemplateParameters() {
        properties.setProviderMode("system");
        stubBirthdayTarget();
        when(consentMapper.selectTarget(13L, 12L, 1)).thenReturn(new CrmMarketingConsentDO()
                .setStatus(CrmMarketingConsentStatusEnum.OPTED_IN.getStatus()));
        when(smsSendApi.sendSingleSmsToAdmin(any())).thenReturn(99L);
        doAnswer(invocation -> {
            invocation.<CrmCustomerCareRecordDO>getArgument(0).setId(15L);
            return 1;
        }).when(recordMapper).insert(any(CrmCustomerCareRecordDO.class));

        service.generateAndSendToday();

        ArgumentCaptor<SmsSendSingleToUserReqDTO> request = ArgumentCaptor.forClass(SmsSendSingleToUserReqDTO.class);
        verify(smsSendApi).sendSingleSmsToAdmin(request.capture());
        assertEquals("客户", request.getValue().getTemplateParams().get("customerName"));
        assertEquals("联系人", request.getValue().getTemplateParams().get("contactName"));
        ArgumentCaptor<CrmCustomerCareRecordDO> record = ArgumentCaptor.forClass(CrmCustomerCareRecordDO.class);
        verify(recordMapper).updateById(record.capture());
        assertEquals(99L, record.getValue().getProviderLogId());
        assertEquals(CrmMarketingRecipientStatusEnum.SENT.getStatus(), record.getValue().getStatus());
    }

    @Test
    void dailyFrequencyLimitSuppressesAutomaticCare() {
        stubBirthdayTarget();
        when(consentMapper.selectTarget(13L, 12L, 1)).thenReturn(new CrmMarketingConsentDO()
                .setStatus(CrmMarketingConsentStatusEnum.OPTED_IN.getStatus()));
        when(recordMapper.countDeliveredSince(eq(13L), eq(12L), eq(1), any())).thenReturn(1L);
        ArgumentCaptor<CrmCustomerCareRecordDO> captor = ArgumentCaptor.forClass(CrmCustomerCareRecordDO.class);

        service.generateAndSendToday();

        verify(recordMapper).insert(captor.capture());
        assertEquals(CrmMarketingRecipientStatusEnum.SUPPRESSED.getStatus(), captor.getValue().getStatus());
        assertTrue(captor.getValue().getFailureReason().contains("频控"));
    }

    @Test
    void disabledPlanDeletesWithConditionalMapperGuard() {
        when(planMapper.selectById(41L)).thenReturn(new CrmCustomerCarePlanDO().setId(41L).setEnabled(false));
        when(planMapper.deleteDisabled(41L)).thenReturn(1);
        service.deletePlan(41L);
        verify(planMapper).deleteDisabled(41L);
    }

    @Test
    void birthdayPageUsesOwnerReadScopeAndComputesNextOccurrence() {
        LocalDate today = LocalDate.now(java.time.ZoneId.of(properties.getCareZone()));
        CrmContactDO contact = new CrmContactDO().setId(51L).setCustomerId(52L).setName("生日联系人")
                .setBirthday(today.minusYears(20));
        when(authorizationService.resolveOwnerReadScope(7L))
                .thenReturn(new CrmOwnerReadScope(false, Set.of(7L)));
        when(contactMapper.selectUpcomingBirthdayPage(any(), eq(today), eq(false), eq(Set.of(7L))))
                .thenReturn(new PageResult<>(List.of(contact), 1L));
        when(customerMapper.selectByIds(anyCollection()))
                .thenReturn(List.of(new CrmCustomerDO().setId(52L).setName("生日客户")));

        PageResult<CrmCustomerBirthdayRespVO> result = service.getBirthdayPage(
                new CrmCustomerBirthdayPageReqVO(), 7L);

        assertEquals("生日客户", result.getList().get(0).getCustomerName());
        assertEquals(0, result.getList().get(0).getDaysUntil());
    }

    @Test
    void customerBirthdayPageUsesCustomerBirthdaySource() {
        LocalDate today = LocalDate.now(java.time.ZoneId.of(properties.getCareZone()));
        CrmCustomerDO customer = new CrmCustomerDO().setId(61L).setName("生日客户")
                .setBirthday(today.minusYears(20)).setMobile("13800000000").setEmail("customer@example.com");
        when(authorizationService.resolveOwnerReadScope(7L))
                .thenReturn(new CrmOwnerReadScope(false, Set.of(7L)));
        when(customerMapper.selectUpcomingBirthdayPage(any(), eq(today), eq(false), eq(Set.of(7L))))
                .thenReturn(new PageResult<>(List.of(customer), 1L));

        CrmCustomerBirthdayPageReqVO request = new CrmCustomerBirthdayPageReqVO();
        request.setTargetType(1);
        PageResult<CrmCustomerBirthdayRespVO> result = service.getBirthdayPage(request, 7L);

        CrmCustomerBirthdayRespVO birthday = result.getList().get(0);
        assertEquals(1, birthday.getTargetType());
        assertEquals(61L, birthday.getCustomerId());
        assertEquals("生日客户", birthday.getCustomerName());
        assertNull(birthday.getContactId());
        assertNull(birthday.getContactName());
        assertEquals(today.minusYears(20), birthday.getBirthday());
        assertEquals(0, birthday.getDaysUntil());
        assertEquals("13800000000", birthday.getMobile());
        assertEquals("customer@example.com", birthday.getEmail());
        verifyNoInteractions(contactMapper);
    }

    private void stubBirthdayTarget() {
        CrmCustomerCarePlanDO plan = new CrmCustomerCarePlanDO().setId(11L).setRuleType(1)
                .setChannel(1).setSmsTemplateCode("birthday").setEnabled(true);
        CrmContactDO contact = new CrmContactDO().setId(12L).setCustomerId(13L)
                .setName("联系人").setMobile("13800000000");
        when(planMapper.selectEnabledByEventDay(anyString())).thenReturn(List.of(plan));
        when(contactMapper.selectBirthdayContacts(anyString())).thenReturn(List.of(contact));
        when(customerMapper.selectById(13L)).thenReturn(new CrmCustomerDO().setId(13L).setName("客户"));
    }

    private static CrmCustomerCarePlanSaveReqVO plan(int ruleType) {
        return new CrmCustomerCarePlanSaveReqVO().setCode(" care ").setName(" 关怀计划 ")
                .setRuleType(ruleType).setChannel(1).setSmsTemplateCode("sms-template").setEnabled(false);
    }
}
