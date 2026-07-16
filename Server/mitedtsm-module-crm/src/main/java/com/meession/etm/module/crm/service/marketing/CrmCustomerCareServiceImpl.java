package com.meession.etm.module.crm.service.marketing;

import com.meession.etm.framework.common.pojo.PageResult;
import com.meession.etm.framework.common.util.object.BeanUtils;
import com.meession.etm.module.crm.controller.admin.marketing.vo.*;
import com.meession.etm.module.crm.dal.dataobject.contact.CrmContactDO;
import com.meession.etm.module.crm.dal.dataobject.customer.CrmCustomerDO;
import com.meession.etm.module.crm.dal.dataobject.marketing.*;
import com.meession.etm.module.crm.dal.mysql.contact.CrmContactMapper;
import com.meession.etm.module.crm.dal.mysql.customer.CrmCustomerMapper;
import com.meession.etm.module.crm.dal.mysql.marketing.*;
import com.meession.etm.module.crm.enums.marketing.CrmMarketingChannelEnum;
import com.meession.etm.module.crm.enums.marketing.CrmMarketingConsentStatusEnum;
import com.meession.etm.module.crm.enums.marketing.CrmMarketingRecipientStatusEnum;
import com.meession.etm.module.crm.framework.marketing.CrmMarketingProperties;
import com.meession.etm.module.system.api.mail.MailSendApi;
import com.meession.etm.module.system.api.mail.dto.MailSendSingleToUserReqDTO;
import com.meession.etm.module.system.api.sms.SmsSendApi;
import com.meession.etm.module.system.api.sms.dto.send.SmsSendSingleToUserReqDTO;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

import static com.meession.etm.framework.common.exception.util.ServiceExceptionUtil.exception;
import static com.meession.etm.module.crm.enums.ErrorCodeConstants.*;

@Service
public class CrmCustomerCareServiceImpl implements CrmCustomerCareService {
    @Resource private CrmCustomerCarePlanMapper planMapper;
    @Resource private CrmCustomerCareRecordMapper recordMapper;
    @Resource private CrmMarketingConsentMapper consentMapper;
    @Resource private CrmCustomerMapper customerMapper;
    @Resource private CrmContactMapper contactMapper;
    @Resource private CrmMarketingProperties properties;
    @Resource private SmsSendApi smsSendApi;
    @Resource private MailSendApi mailSendApi;
    @Resource private ObjectMapper objectMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long savePlan(CrmCustomerCarePlanSaveReqVO request) {
        if (request.getRuleType() == null || (request.getRuleType() != 1 && request.getRuleType() != 2)
                || !request.getEventMonthDay().matches("\\d{2}-\\d{2}")) throw exception(MARKETING_CHANNEL_INVALID);
        if (request.getChannel() < 1 || request.getChannel() > 3) throw exception(MARKETING_CHANNEL_INVALID);
        if ((request.getChannel() == 1 || request.getChannel() == 3) && isBlank(request.getSmsTemplateCode())) throw exception(MARKETING_TEMPLATE_REQUIRED);
        if ((request.getChannel() == 2 || request.getChannel() == 3) && isBlank(request.getMailTemplateCode())) throw exception(MARKETING_TEMPLATE_REQUIRED);
        CrmCustomerCarePlanDO same = planMapper.selectByCode(request.getCode());
        if (same != null && !same.getId().equals(request.getId())) throw exception(MARKETING_CARE_PLAN_CODE_EXISTS);
        CrmCustomerCarePlanDO row = BeanUtils.toBean(request, CrmCustomerCarePlanDO.class);
        if (request.getId() == null) planMapper.insert(row); else {
            if (planMapper.selectById(request.getId()) == null) throw exception(MARKETING_CARE_PLAN_NOT_EXISTS);
            planMapper.updateById(row);
        }
        return row.getId();
    }

    @Override public PageResult<CrmCustomerCarePlanDO> getPlanPage(CrmCustomerCarePlanPageReqVO request) { return planMapper.selectPage(request); }
    @Override public PageResult<CrmCustomerCareRecordDO> getRecordPage(CrmCustomerCareRecordPageReqVO request) { return recordMapper.selectPage(request); }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int generateAndSendToday() {
        LocalDate today = LocalDate.now();
        String monthDay = String.format("%02d-%02d", today.getMonthValue(), today.getDayOfMonth());
        List<CrmCustomerCarePlanDO> plans = planMapper.selectEnabledByEventDay(monthDay);
        int generated = 0;
        for (CrmCustomerCarePlanDO plan : plans) {
            List<CrmContactDO> contacts = plan.getRuleType() == 1 ? contactMapper.selectBirthdayContacts(monthDay)
                    : contactMapper.selectPrimaryContactListByCustomerIds(customerMapper.selectList().stream().map(CrmCustomerDO::getId).toList());
            for (CrmContactDO contact : contacts) {
                CrmMarketingConsentDO consentSms = resolveConsent(contact.getCustomerId(), contact.getId(), 1);
                CrmMarketingConsentDO consentMail = resolveConsent(contact.getCustomerId(), contact.getId(), 2);
                List<Integer> channels = plan.getChannel() == 3 ? List.of(1, 2) : List.of(plan.getChannel());
                for (Integer channel : channels) {
                    CrmMarketingConsentDO consent = channel == 1 ? consentSms : consentMail;
                    CrmCustomerCareRecordDO record = new CrmCustomerCareRecordDO().setPlanId(plan.getId())
                            .setCustomerId(contact.getCustomerId()).setContactId(contact.getId()).setEventDate(today)
                            .setChannel(channel).setStatus(CrmMarketingRecipientStatusEnum.PENDING.getStatus());
                    if (consent == null || CrmMarketingConsentStatusEnum.OPTED_OUT.getStatus().equals(consent.getStatus()))
                        record.setStatus(CrmMarketingRecipientStatusEnum.SUPPRESSED.getStatus()).setFailureReason("未获得同意或已退订");
                    try { recordMapper.insert(record); generated++; } catch (RuntimeException duplicate) { /* unique rule suppresses repeats */ }
                    if (record.getId() != null && record.getStatus().equals(CrmMarketingRecipientStatusEnum.PENDING.getStatus())) sendRecord(record, plan, contact);
                }
            }
        }
        return generated;
    }

    private void sendRecord(CrmCustomerCareRecordDO record, CrmCustomerCarePlanDO plan, CrmContactDO contact) {
        try {
            if ("record-only".equals(properties.getProviderMode())) {
                record.setStatus(CrmMarketingRecipientStatusEnum.RECORDED.getStatus()).setSentAt(LocalDateTime.now());
            } else if (record.getChannel() == 1) {
                if (contact.getMobile() == null || contact.getMobile().isBlank()) throw new IllegalStateException("手机号为空");
                SmsSendSingleToUserReqDTO request = new SmsSendSingleToUserReqDTO(); request.setMobile(contact.getMobile()); request.setTemplateCode(plan.getSmsTemplateCode());
                record.setProviderLogId(smsSendApi.sendSingleSmsToAdmin(request)).setStatus(CrmMarketingRecipientStatusEnum.SENT.getStatus()).setSentAt(LocalDateTime.now());
            } else {
                if (contact.getEmail() == null || contact.getEmail().isBlank()) throw new IllegalStateException("邮箱为空");
                MailSendSingleToUserReqDTO request = new MailSendSingleToUserReqDTO(); request.setToMails(List.of(contact.getEmail())); request.setTemplateCode(plan.getMailTemplateCode());
                record.setProviderLogId(mailSendApi.sendSingleMailToAdmin(request)).setStatus(CrmMarketingRecipientStatusEnum.SENT.getStatus()).setSentAt(LocalDateTime.now());
            }
        } catch (RuntimeException ex) { record.setStatus(CrmMarketingRecipientStatusEnum.FAILED.getStatus()).setFailureReason(ex.getMessage()); }
        recordMapper.updateById(record);
    }

    private static boolean isBlank(String value) { return value == null || value.isBlank(); }

    CrmMarketingConsentDO resolveConsent(Long customerId, Long contactId, Integer channel) {
        CrmMarketingConsentDO consent = consentMapper.selectTarget(customerId, contactId, channel);
        return consent != null ? consent : consentMapper.selectTarget(customerId, null, channel);
    }
}
