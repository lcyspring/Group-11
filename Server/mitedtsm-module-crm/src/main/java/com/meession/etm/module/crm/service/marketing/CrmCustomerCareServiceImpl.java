package com.meession.etm.module.crm.service.marketing;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.meession.etm.framework.common.pojo.PageResult;
import com.meession.etm.framework.common.util.object.BeanUtils;
import com.meession.etm.module.crm.controller.admin.marketing.vo.*;
import com.meession.etm.module.crm.dal.dataobject.contact.CrmContactDO;
import com.meession.etm.module.crm.dal.dataobject.customer.CrmCustomerDO;
import com.meession.etm.module.crm.dal.dataobject.marketing.*;
import com.meession.etm.module.crm.dal.mysql.contact.CrmContactMapper;
import com.meession.etm.module.crm.dal.mysql.customer.CrmCustomerMapper;
import com.meession.etm.module.crm.dal.mysql.marketing.*;
import com.meession.etm.module.crm.enums.marketing.CrmCustomerCareRuleTypeEnum;
import com.meession.etm.module.crm.enums.marketing.CrmMarketingConsentStatusEnum;
import com.meession.etm.module.crm.enums.marketing.CrmMarketingRecipientStatusEnum;
import com.meession.etm.module.crm.framework.marketing.CrmMarketingProperties;
import com.meession.etm.module.crm.framework.permission.CrmAuthorizationService;
import com.meession.etm.module.system.api.mail.MailSendApi;
import com.meession.etm.module.system.api.mail.dto.MailSendSingleToUserReqDTO;
import com.meession.etm.module.system.api.sms.SmsSendApi;
import com.meession.etm.module.system.api.sms.dto.send.SmsSendSingleToUserReqDTO;
import jakarta.annotation.Resource;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.meession.etm.framework.common.exception.util.ServiceExceptionUtil.exception;
import static com.meession.etm.module.crm.enums.ErrorCodeConstants.*;

@Service
public class CrmCustomerCareServiceImpl implements CrmCustomerCareService {
    private static final int LIFECYCLE_DEAL = 30;
    private static final DateTimeFormatter MONTH_DAY = DateTimeFormatter.ofPattern("MM-dd");

    @Resource private CrmCustomerCarePlanMapper planMapper;
    @Resource private CrmCustomerCareRecordMapper recordMapper;
    @Resource private CrmMarketingBroadcastRecipientMapper broadcastRecipientMapper;
    @Resource private CrmMarketingConsentMapper consentMapper;
    @Resource private CrmCustomerMapper customerMapper;
    @Resource private CrmContactMapper contactMapper;
    @Resource private CrmAuthorizationService authorizationService;
    @Resource private CrmMarketingProperties properties;
    @Resource private SmsSendApi smsSendApi;
    @Resource private MailSendApi mailSendApi;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long savePlan(CrmCustomerCarePlanSaveReqVO request) {
        validatePlan(request);
        CrmCustomerCarePlanDO same = planMapper.selectByCode(request.getCode().trim());
        if (same != null && !same.getId().equals(request.getId())) {
            throw exception(MARKETING_CARE_PLAN_CODE_EXISTS);
        }
        CrmCustomerCarePlanDO row = BeanUtils.toBean(request, CrmCustomerCarePlanDO.class)
                .setCode(request.getCode().trim())
                .setName(request.getName().trim())
                .setTargetScope(targetScope(request.getRuleType()));
        normalizeRuleFields(row);
        if (request.getId() == null) {
            planMapper.insert(row);
        } else {
            requirePlan(request.getId());
            planMapper.updateById(row);
        }
        return row.getId();
    }

    @Override
    public CrmCustomerCarePlanDO getPlan(Long id) {
        return requirePlan(id);
    }

    @Override
    public void updatePlanStatus(CrmCustomerCarePlanStatusReqVO request) {
        requirePlan(request.getId());
        if (planMapper.updateEnabled(request.getId(), request.getEnabled()) != 1) {
            throw exception(MARKETING_CARE_PLAN_NOT_EXISTS);
        }
    }

    @Override
    public void deletePlan(Long id) {
        CrmCustomerCarePlanDO plan = requirePlan(id);
        if (Boolean.TRUE.equals(plan.getEnabled())) {
            throw exception(MARKETING_CARE_PLAN_ENABLED);
        }
        if (planMapper.deleteDisabled(id) != 1) {
            throw exception(MARKETING_CARE_PLAN_ENABLED);
        }
    }

    @Override
    public PageResult<CrmCustomerCarePlanDO> getPlanPage(CrmCustomerCarePlanPageReqVO request) {
        return planMapper.selectPage(request);
    }

    @Override
    public PageResult<CrmCustomerCareRecordRespVO> getRecordPage(CrmCustomerCareRecordPageReqVO request,
                                                                  Long userId) {
        var scope = authorizationService.resolveOwnerReadScope(userId);
        PageResult<CrmCustomerCareRecordDO> page = recordMapper.selectPage(request, scope.all(), scope.ownerUserIds());
        List<CrmCustomerCareRecordRespVO> responses = BeanUtils.toBean(page.getList(), CrmCustomerCareRecordRespVO.class);
        Set<Long> planIds = ids(page.getList(), CrmCustomerCareRecordDO::getPlanId);
        Set<Long> customerIds = ids(page.getList(), CrmCustomerCareRecordDO::getCustomerId);
        Set<Long> contactIds = ids(page.getList(), CrmCustomerCareRecordDO::getContactId);
        Map<Long, String> plans = names(planIds.isEmpty() ? List.of() : planMapper.selectBatchIds(planIds),
                CrmCustomerCarePlanDO::getId, CrmCustomerCarePlanDO::getName);
        Map<Long, String> customers = names(customerIds.isEmpty() ? List.of() : customerMapper.selectBatchIds(customerIds),
                CrmCustomerDO::getId, CrmCustomerDO::getName);
        Map<Long, String> contacts = names(contactIds.isEmpty() ? List.of() : contactMapper.selectBatchIds(contactIds),
                CrmContactDO::getId, CrmContactDO::getName);
        responses.forEach(item -> item.setPlanName(plans.get(item.getPlanId()))
                .setCustomerName(customers.get(item.getCustomerId()))
                .setContactName(contacts.get(item.getContactId())));
        return new PageResult<>(responses, page.getTotal());
    }

    @Override
    public PageResult<CrmCustomerBirthdayRespVO> getBirthdayPage(CrmCustomerBirthdayPageReqVO request,
                                                                  Long userId) {
        LocalDate today = today();
        var scope = authorizationService.resolveOwnerReadScope(userId);
        if (Integer.valueOf(1).equals(request.getTargetType())) {
            PageResult<CrmCustomerDO> page = customerMapper.selectUpcomingBirthdayPage(request, today,
                    scope.all(), scope.ownerUserIds());
            List<CrmCustomerBirthdayRespVO> list = page.getList().stream().map(customer -> {
                LocalDate next = nextBirthday(customer.getBirthday(), today);
                return new CrmCustomerBirthdayRespVO().setTargetType(1).setCustomerId(customer.getId())
                        .setCustomerName(customer.getName()).setBirthday(customer.getBirthday()).setNextBirthday(next)
                        .setDaysUntil((int) ChronoUnit.DAYS.between(today, next))
                        .setMobile(customer.getMobile()).setEmail(customer.getEmail());
            }).toList();
            return new PageResult<>(list, page.getTotal());
        }
        PageResult<CrmContactDO> page = contactMapper.selectUpcomingBirthdayPage(request, today,
                scope.all(), scope.ownerUserIds());
        Set<Long> customerIds = ids(page.getList(), CrmContactDO::getCustomerId);
        Map<Long, String> customers = names(customerIds.isEmpty() ? List.of() : customerMapper.selectBatchIds(customerIds),
                CrmCustomerDO::getId, CrmCustomerDO::getName);
        List<CrmCustomerBirthdayRespVO> list = page.getList().stream().map(contact -> {
            LocalDate next = nextBirthday(contact.getBirthday(), today);
            return new CrmCustomerBirthdayRespVO().setTargetType(2).setCustomerId(contact.getCustomerId())
                    .setCustomerName(customers.get(contact.getCustomerId()))
                    .setContactId(contact.getId()).setContactName(contact.getName())
                    .setBirthday(contact.getBirthday()).setNextBirthday(next)
                    .setDaysUntil((int) ChronoUnit.DAYS.between(today, next))
                    .setMobile(contact.getMobile()).setEmail(contact.getEmail());
        }).toList();
        return new PageResult<>(list, page.getTotal());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int generateAndSendToday() {
        LocalDate today = today();
        List<CrmCustomerCarePlanDO> plans = planMapper.selectEnabledByEventDay(today.format(MONTH_DAY));
        int generated = 0;
        for (CrmCustomerCarePlanDO plan : plans) {
            for (CrmContactDO contact : resolveContacts(plan, today)) {
                CrmCustomerDO customer = customerMapper.selectById(contact.getCustomerId());
                if (customer == null) continue;
                for (Integer channel : channels(plan.getChannel())) {
                    CrmCustomerCareRecordDO record = buildRecord(plan, customer, contact, today, channel);
                    try {
                        recordMapper.insert(record);
                        generated++;
                    } catch (DuplicateKeyException duplicate) {
                        continue;
                    }
                    if (CrmMarketingRecipientStatusEnum.PENDING.getStatus().equals(record.getStatus())) {
                        sendRecord(record, plan, customer, contact);
                    }
                }
            }
        }
        return generated;
    }

    private List<CrmContactDO> resolveContacts(CrmCustomerCarePlanDO plan, LocalDate today) {
        if (CrmCustomerCareRuleTypeEnum.BIRTHDAY.getType().equals(plan.getRuleType())) {
            return contactMapper.selectBirthdayContacts(today.format(MONTH_DAY));
        }
        if (CrmCustomerCareRuleTypeEnum.HOLIDAY.getType().equals(plan.getRuleType())) {
            return contactMapper.selectPrimaryContactsByLifecycleStatus(LIFECYCLE_DEAL);
        }
        LocalDate targetDate = today.minusDays(plan.getFollowUpDays());
        return contactMapper.selectPrimaryContactsByLifecycleChangedBetween(LIFECYCLE_DEAL,
                targetDate.atStartOfDay(), targetDate.plusDays(1).atStartOfDay());
    }

    private CrmCustomerCareRecordDO buildRecord(CrmCustomerCarePlanDO plan, CrmCustomerDO customer,
                                                 CrmContactDO contact, LocalDate eventDate, Integer channel) {
        CrmCustomerCareRecordDO record = new CrmCustomerCareRecordDO().setPlanId(plan.getId())
                .setCustomerId(customer.getId()).setContactId(contact.getId()).setEventDate(eventDate)
                .setChannel(channel).setStatus(CrmMarketingRecipientStatusEnum.PENDING.getStatus());
        CrmMarketingConsentDO consent = resolveConsent(customer.getId(), contact.getId(), channel);
        if (consent == null || CrmMarketingConsentStatusEnum.OPTED_OUT.getStatus().equals(consent.getStatus())) {
            return record.setStatus(CrmMarketingRecipientStatusEnum.SUPPRESSED.getStatus())
                    .setFailureReason("未获得该渠道同意或已退订");
        }
        if (channel == 1 && isBlank(contact.getMobile())) {
            return record.setStatus(CrmMarketingRecipientStatusEnum.SUPPRESSED.getStatus()).setFailureReason("手机号为空");
        }
        if (channel == 2 && isBlank(contact.getEmail())) {
            return record.setStatus(CrmMarketingRecipientStatusEnum.SUPPRESSED.getStatus()).setFailureReason("邮箱为空");
        }
        if (quotaExceeded(record)) {
            return record.setStatus(CrmMarketingRecipientStatusEnum.SUPPRESSED.getStatus())
                    .setFailureReason("超过营销触达频控或月度配额");
        }
        return record;
    }

    private boolean quotaExceeded(CrmCustomerCareRecordDO record) {
        LocalDateTime dayStart = today().atStartOfDay();
        long careDaily = recordMapper.countDeliveredSince(record.getCustomerId(), record.getContactId(),
                record.getChannel(), dayStart);
        long broadcastDaily = broadcastRecipientMapper.selectCount(new LambdaQueryWrapper<CrmMarketingBroadcastRecipientDO>()
                .eq(CrmMarketingBroadcastRecipientDO::getCustomerId, record.getCustomerId())
                .eq(CrmMarketingBroadcastRecipientDO::getContactId, record.getContactId())
                .eq(CrmMarketingBroadcastRecipientDO::getChannel, record.getChannel())
                .ge(CrmMarketingBroadcastRecipientDO::getSentAt, dayStart)
                .in(CrmMarketingBroadcastRecipientDO::getStatus,
                        CrmMarketingRecipientStatusEnum.SENT.getStatus(), CrmMarketingRecipientStatusEnum.RECORDED.getStatus()));
        if (careDaily + broadcastDaily >= properties.getPerRecipientDailyLimit()) return true;
        LocalDateTime monthStart = YearMonth.from(today()).atDay(1).atStartOfDay();
        long careMonthly = recordMapper.countDeliveredSince(monthStart);
        long broadcastMonthly = broadcastRecipientMapper.selectCount(new LambdaQueryWrapper<CrmMarketingBroadcastRecipientDO>()
                .ge(CrmMarketingBroadcastRecipientDO::getSentAt, monthStart)
                .in(CrmMarketingBroadcastRecipientDO::getStatus,
                        CrmMarketingRecipientStatusEnum.SENT.getStatus(), CrmMarketingRecipientStatusEnum.RECORDED.getStatus()));
        return careMonthly + broadcastMonthly >= properties.getMonthlyRecipientLimit();
    }

    private void sendRecord(CrmCustomerCareRecordDO record, CrmCustomerCarePlanDO plan,
                            CrmCustomerDO customer, CrmContactDO contact) {
        try {
            if ("record-only".equals(properties.getProviderMode())) {
                record.setStatus(CrmMarketingRecipientStatusEnum.RECORDED.getStatus()).setSentAt(now());
            } else {
                Map<String, Object> params = Map.of(
                        "customerName", Optional.ofNullable(customer.getName()).orElse(""),
                        "contactName", Optional.ofNullable(contact.getName()).orElse(""),
                        "eventDate", record.getEventDate().toString());
                Long logId;
                if (record.getChannel() == 1) {
                    SmsSendSingleToUserReqDTO request = new SmsSendSingleToUserReqDTO();
                    request.setMobile(contact.getMobile());
                    request.setTemplateCode(plan.getSmsTemplateCode());
                    request.setTemplateParams(params);
                    logId = smsSendApi.sendSingleSmsToAdmin(request);
                } else {
                    MailSendSingleToUserReqDTO request = new MailSendSingleToUserReqDTO();
                    request.setToMails(List.of(contact.getEmail()));
                    request.setTemplateCode(plan.getMailTemplateCode());
                    request.setTemplateParams(params);
                    logId = mailSendApi.sendSingleMailToAdmin(request);
                }
                if (logId == null) throw new IllegalStateException("消息提供商未返回发送日志编号");
                record.setProviderLogId(logId).setStatus(CrmMarketingRecipientStatusEnum.SENT.getStatus()).setSentAt(now());
            }
        } catch (RuntimeException ex) {
            record.setStatus(CrmMarketingRecipientStatusEnum.FAILED.getStatus()).setFailureReason(ex.getMessage());
        }
        recordMapper.updateById(record);
    }

    private void validatePlan(CrmCustomerCarePlanSaveReqVO request) {
        if (!CrmCustomerCareRuleTypeEnum.isValid(request.getRuleType())) {
            throw exception(MARKETING_CARE_RULE_INVALID);
        }
        if (request.getChannel() == null || request.getChannel() < 1 || request.getChannel() > 3) {
            throw exception(MARKETING_CHANNEL_INVALID);
        }
        if ((request.getChannel() == 1 || request.getChannel() == 3) && isBlank(request.getSmsTemplateCode())) {
            throw exception(MARKETING_TEMPLATE_REQUIRED);
        }
        if ((request.getChannel() == 2 || request.getChannel() == 3) && isBlank(request.getMailTemplateCode())) {
            throw exception(MARKETING_TEMPLATE_REQUIRED);
        }
        if (CrmCustomerCareRuleTypeEnum.HOLIDAY.getType().equals(request.getRuleType())) {
            validateMonthDay(request.getEventMonthDay());
        } else if (CrmCustomerCareRuleTypeEnum.POST_DEAL_FOLLOW_UP.getType().equals(request.getRuleType())
                && (request.getFollowUpDays() == null || request.getFollowUpDays() < 1 || request.getFollowUpDays() > 3650)) {
            throw exception(MARKETING_CARE_RULE_INVALID);
        }
    }

    private void validateMonthDay(String value) {
        if (isBlank(value)) throw exception(MARKETING_CARE_RULE_INVALID);
        try {
            LocalDate.parse("2000-" + value, DateTimeFormatter.ISO_LOCAL_DATE);
        } catch (DateTimeParseException ex) {
            throw exception(MARKETING_CARE_RULE_INVALID);
        }
    }

    private void normalizeRuleFields(CrmCustomerCarePlanDO row) {
        if (!CrmCustomerCareRuleTypeEnum.HOLIDAY.getType().equals(row.getRuleType())) row.setEventMonthDay(null);
        if (!CrmCustomerCareRuleTypeEnum.POST_DEAL_FOLLOW_UP.getType().equals(row.getRuleType())) row.setFollowUpDays(null);
    }

    private String targetScope(Integer ruleType) {
        return switch (ruleType) {
            case 1 -> "BIRTHDAY_CONTACTS";
            case 2, 3 -> "DEAL_CUSTOMERS";
            default -> throw exception(MARKETING_CARE_RULE_INVALID);
        };
    }

    private CrmCustomerCarePlanDO requirePlan(Long id) {
        CrmCustomerCarePlanDO plan = planMapper.selectById(id);
        if (plan == null) throw exception(MARKETING_CARE_PLAN_NOT_EXISTS);
        return plan;
    }

    private CrmMarketingConsentDO resolveConsent(Long customerId, Long contactId, Integer channel) {
        CrmMarketingConsentDO consent = consentMapper.selectTarget(customerId, contactId, channel);
        return consent != null ? consent : consentMapper.selectTarget(customerId, null, channel);
    }

    private List<Integer> channels(Integer channel) {
        return channel == 3 ? List.of(1, 2) : List.of(channel);
    }

    private LocalDate today() {
        return LocalDate.now(ZoneId.of(properties.getCareZone()));
    }

    private LocalDateTime now() {
        return LocalDateTime.now(ZoneId.of(properties.getCareZone()));
    }

    static LocalDate nextBirthday(LocalDate birthday, LocalDate today) {
        int year = today.getYear();
        LocalDate candidate = birthdayAtYear(birthday, year);
        return candidate.isBefore(today) ? birthdayAtYear(birthday, year + 1) : candidate;
    }

    private static LocalDate birthdayAtYear(LocalDate birthday, int year) {
        if (birthday.getMonthValue() == 2 && birthday.getDayOfMonth() == 29 && !Year.isLeap(year)) {
            return LocalDate.of(year, 2, 28);
        }
        return birthday.withYear(year);
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private static <T> Set<Long> ids(Collection<T> rows, Function<T, Long> getter) {
        return rows.stream().map(getter).filter(Objects::nonNull).collect(Collectors.toSet());
    }

    private static <T> Map<Long, String> names(Collection<T> rows, Function<T, Long> id,
                                                Function<T, String> name) {
        return rows.stream().collect(Collectors.toMap(id, name, (first, ignored) -> first));
    }
}
