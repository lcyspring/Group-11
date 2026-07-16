package com.meession.etm.module.crm.service.marketing;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.meession.etm.framework.common.pojo.PageResult;
import com.meession.etm.framework.common.util.object.BeanUtils;
import com.meession.etm.module.crm.controller.admin.marketing.vo.*;
import com.meession.etm.module.crm.dal.dataobject.contact.CrmContactDO;
import com.meession.etm.module.crm.dal.dataobject.customer.CrmCustomerDO;
import com.meession.etm.module.crm.dal.dataobject.marketing.*;
import com.meession.etm.module.crm.dal.mysql.contact.CrmContactMapper;
import com.meession.etm.module.crm.dal.mysql.customer.CrmCustomerMapper;
import com.meession.etm.module.crm.dal.mysql.marketing.*;
import com.meession.etm.module.crm.enums.marketing.*;
import com.meession.etm.module.crm.framework.marketing.CrmMarketingProperties;
import com.meession.etm.module.crm.framework.permission.CrmAuthorizationService;
import com.meession.etm.module.system.api.mail.MailSendApi;
import com.meession.etm.module.system.api.mail.dto.MailSendSingleToUserReqDTO;
import com.meession.etm.module.system.api.sms.SmsSendApi;
import com.meession.etm.module.system.api.sms.dto.send.SmsSendSingleToUserReqDTO;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.*;

import static com.meession.etm.framework.common.exception.util.ServiceExceptionUtil.exception;
import static com.meession.etm.module.crm.enums.ErrorCodeConstants.*;

@Service
public class CrmMarketingOutreachService {
    @Resource private CrmMarketingBroadcastMapper broadcastMapper;
    @Resource private CrmMarketingBroadcastRecipientMapper recipientMapper;
    @Resource private CrmMarketingConsentMapper consentMapper;
    @Resource private CrmCustomerMapper customerMapper;
    @Resource private CrmContactMapper contactMapper;
    @Resource private CrmAuthorizationService authorizationService;
    @Resource private CrmMarketingProperties properties;
    @Resource private SmsSendApi smsSendApi;
    @Resource private MailSendApi mailSendApi;
    @Resource private ObjectMapper objectMapper;

    @Transactional(rollbackFor = Exception.class)
    public Long saveBroadcast(CrmMarketingBroadcastSaveReqVO request, Long userId) {
        validateChannel(request.getChannel(), request.getSmsTemplateCode(), request.getMailTemplateCode());
        CrmMarketingBroadcastDO existing = request.getId() == null ? null : requireBroadcast(request.getId());
        if (existing != null) {
            requireCreatorOrAdmin(existing, userId);
            if (!List.of(CrmMarketingBroadcastStatusEnum.DRAFT.getStatus(),
                    CrmMarketingBroadcastStatusEnum.REJECTED.getStatus()).contains(existing.getStatus())) {
                throw exception(MARKETING_BROADCAST_STATUS_INVALID);
            }
        }
        CrmMarketingBroadcastDO row = BeanUtils.toBean(request, CrmMarketingBroadcastDO.class);
        row.setStatus(CrmMarketingBroadcastStatusEnum.DRAFT.getStatus());
        if (request.getId() == null) broadcastMapper.insert(row); else {
            row.setId(request.getId());
            if (broadcastMapper.updateEditable(row, List.of(CrmMarketingBroadcastStatusEnum.DRAFT.getStatus(),
                    CrmMarketingBroadcastStatusEnum.REJECTED.getStatus())) == 0) {
                throw exception(MARKETING_BROADCAST_STATUS_INVALID);
            }
            recipientMapper.delete(CrmMarketingBroadcastRecipientDO::getBroadcastId, row.getId());
        }
        List<CrmMarketingBroadcastRecipientDO> recipients = resolveRecipients(request, row.getId(), userId);
        recipients.forEach(recipientMapper::insert);
        row.setTotalCount(recipients.size());
        row.setValidCount((int) recipients.stream().filter(item -> item.getStatus().equals(CrmMarketingRecipientStatusEnum.PENDING.getStatus())).count());
        row.setSuppressedCount(recipients.size() - row.getValidCount());
        row.setSentCount(0).setFailedCount(0);
        broadcastMapper.updateById(row);
        if (row.getValidCount() == 0) throw exception(MARKETING_RECIPIENT_NOT_FOUND);
        return row.getId();
    }

    public PageResult<CrmMarketingBroadcastDO> getBroadcastPage(CrmMarketingBroadcastPageReqVO request) {
        return broadcastMapper.selectPage(request);
    }

    public List<Long> getDueScheduledBroadcastIds() {
        return broadcastMapper.selectDueScheduled(CrmMarketingBroadcastStatusEnum.READY.getStatus(),
                LocalDateTime.now(), properties.getMaxBatchSize()).stream().map(CrmMarketingBroadcastDO::getId).toList();
    }

    public List<CrmCustomerDO> getTargetCustomers(Long userId) {
        if (authorizationService.isCrmAdmin(userId)) {
            return customerMapper.selectList();
        }
        var readScope = authorizationService.resolveOwnerReadScope(userId);
        if (readScope.all()) {
            return customerMapper.selectList();
        }
        if (readScope.ownerUserIds().isEmpty()) {
            return List.of();
        }
        return customerMapper.selectList(new com.meession.etm.framework.mybatis.core.query.LambdaQueryWrapperX<CrmCustomerDO>()
                .in(CrmCustomerDO::getOwnerUserId, readScope.ownerUserIds())
                .orderByAsc(CrmCustomerDO::getName)
                .orderByAsc(CrmCustomerDO::getId));
    }

    public List<CrmContactDO> getTargetContacts(List<CrmCustomerDO> customers) {
        if (customers.isEmpty()) {
            return List.of();
        }
        Set<Long> customerIds = customers.stream().map(CrmCustomerDO::getId).collect(java.util.stream.Collectors.toSet());
        return contactMapper.selectList(new com.meession.etm.framework.mybatis.core.query.LambdaQueryWrapperX<CrmContactDO>()
                .in(CrmContactDO::getCustomerId, customerIds)
                .orderByAsc(CrmContactDO::getName)
                .orderByAsc(CrmContactDO::getId));
    }

    public CrmMarketingBroadcastDO getBroadcast(Long id) {
        return requireBroadcast(id);
    }

    public List<CrmMarketingBroadcastRecipientDO> getBroadcastRecipients(Long id) {
        requireBroadcast(id);
        return recipientMapper.selectList(CrmMarketingBroadcastRecipientDO::getBroadcastId, id);
    }

    @Transactional(rollbackFor = Exception.class)
    public void deleteBroadcast(Long id, Long userId) {
        CrmMarketingBroadcastDO row = requireBroadcast(id);
        requireCreatorOrAdmin(row, userId);
        if (broadcastMapper.deleteDraft(id, CrmMarketingBroadcastStatusEnum.DRAFT.getStatus()) == 0) {
            throw exception(MARKETING_BROADCAST_STATUS_INVALID);
        }
        recipientMapper.delete(CrmMarketingBroadcastRecipientDO::getBroadcastId, id);
    }

    public PageResult<CrmMarketingBroadcastRecipientDO> getRecipientPage(CrmMarketingRecipientPageReqVO request) {
        return recipientMapper.selectPage(request);
    }

    public void submitReview(Long id, Long userId) {
        CrmMarketingBroadcastDO row = requireBroadcast(id);
        requireCreatorOrAdmin(row, userId);
        if (broadcastMapper.transition(id, List.of(CrmMarketingBroadcastStatusEnum.DRAFT.getStatus()),
                CrmMarketingBroadcastStatusEnum.PENDING_REVIEW.getStatus()) == 0) {
            throw exception(MARKETING_BROADCAST_STATUS_INVALID);
        }
    }

    public void review(CrmMarketingReviewReqVO request, Long reviewerUserId, boolean approved) {
        CrmMarketingBroadcastDO row = requireBroadcast(request.getId());
        if (!CrmMarketingBroadcastStatusEnum.PENDING_REVIEW.getStatus().equals(row.getStatus()))
            throw exception(MARKETING_BROADCAST_STATUS_INVALID);
        if (Objects.equals(parseCreatorUserId(row.getCreator()), reviewerUserId)) throw exception(MARKETING_REVIEWER_INVALID);
        if (!approved && (request.getComment() == null || request.getComment().isBlank())) {
            throw exception(MARKETING_REVIEW_COMMENT_REQUIRED);
        }
        Integer target = approved ? CrmMarketingBroadcastStatusEnum.READY.getStatus()
                : CrmMarketingBroadcastStatusEnum.REJECTED.getStatus();
        if (broadcastMapper.reviewIfPending(row.getId(), reviewerUserId, LocalDateTime.now(),
                request.getComment(), target, CrmMarketingBroadcastStatusEnum.PENDING_REVIEW.getStatus()) == 0) {
            throw exception(MARKETING_BROADCAST_STATUS_INVALID);
        }
    }

    @Transactional(rollbackFor = Exception.class)
    public void send(Long id) {
        CrmMarketingBroadcastDO row = requireBroadcast(id);
        if (!CrmMarketingBroadcastStatusEnum.READY.getStatus().equals(row.getStatus())
                && !CrmMarketingBroadcastStatusEnum.PARTIAL_FAILED.getStatus().equals(row.getStatus()))
            throw exception(MARKETING_BROADCAST_STATUS_INVALID);
        if (row.getScheduledAt() != null && row.getScheduledAt().isAfter(LocalDateTime.now()))
            throw exception(MARKETING_BROADCAST_STATUS_INVALID);
        enforceQuota(row.getId());
        if (broadcastMapper.transition(row.getId(), List.of(CrmMarketingBroadcastStatusEnum.READY.getStatus(),
                CrmMarketingBroadcastStatusEnum.PARTIAL_FAILED.getStatus()),
                CrmMarketingBroadcastStatusEnum.SENDING.getStatus()) == 0) {
            throw exception(MARKETING_BROADCAST_STATUS_INVALID);
        }
        row.setStatus(CrmMarketingBroadcastStatusEnum.SENDING.getStatus());
        sendRecipients(row);
    }

    @Transactional(rollbackFor = Exception.class)
    public void retryFailed(Long id) {
        CrmMarketingBroadcastDO row = requireBroadcast(id);
        if (!CrmMarketingBroadcastStatusEnum.PARTIAL_FAILED.getStatus().equals(row.getStatus()))
            throw exception(MARKETING_BROADCAST_STATUS_INVALID);
        if (recipientMapper.resetFailed(id) == 0) throw exception(MARKETING_RECIPIENT_NOT_FOUND);
        send(id);
    }

    @Transactional(rollbackFor = Exception.class)
    public void saveConsent(CrmMarketingConsentSaveReqVO request, Long userId) {
        if (request.getChannel() < 1 || request.getChannel() > 2 || request.getStatus() < 1 || request.getStatus() > 2)
            throw exception(MARKETING_CHANNEL_INVALID);
        CrmCustomerDO customer = customerMapper.selectById(request.getCustomerId());
        if (customer == null) throw exception(MARKETING_RECIPIENT_NOT_FOUND);
        if (!authorizationService.isCrmAdmin(userId)
                && !authorizationService.resolveOwnerReadScope(userId).allows(customer.getOwnerUserId()))
            throw exception(MARKETING_PERMISSION_DENIED);
        CrmMarketingConsentDO existing = consentMapper.selectTarget(request.getCustomerId(), request.getContactId(), request.getChannel());
        CrmMarketingConsentDO row = BeanUtils.toBean(request, CrmMarketingConsentDO.class)
                .setOccurredAt(LocalDateTime.now()).setSource(Optional.ofNullable(request.getSource()).orElse("crm-admin"));
        if (existing == null) consentMapper.insert(row); else { row.setId(existing.getId()); consentMapper.updateById(row); }
    }

    private List<CrmMarketingBroadcastRecipientDO> resolveRecipients(CrmMarketingBroadcastSaveReqVO request,
                                                                       Long broadcastId, Long userId) {
        LinkedHashMap<String, CrmMarketingBroadcastRecipientDO> result = new LinkedHashMap<>();
        Set<Long> customerIds = new LinkedHashSet<>(Optional.ofNullable(request.getCustomerIds()).orElse(List.of()));
        Set<Long> contactIds = new LinkedHashSet<>(Optional.ofNullable(request.getContactIds()).orElse(List.of()));
        List<CrmCustomerDO> customers = customerIds.isEmpty() ? List.of() : customerMapper.selectBatchIds(customerIds);
        Map<Long, CrmCustomerDO> customerMap = new HashMap<>();
        customers.forEach(customer -> { checkReadable(customer, userId); customerMap.put(customer.getId(), customer); });
        List<CrmContactDO> contacts = contactIds.isEmpty() ? List.of() : contactMapper.selectBatchIds(contactIds);
        for (CrmContactDO contact : contacts) {
            CrmCustomerDO customer = customerMap.computeIfAbsent(contact.getCustomerId(), customerMapper::selectById);
            if (customer != null) { checkReadable(customer, userId); addTarget(result, request, broadcastId, customer, contact); }
        }
        for (CrmCustomerDO customer : customers) {
            CrmContactDO primary = contactMapper.selectPrimaryContactListByCustomerIds(List.of(customer.getId())).stream().findFirst().orElse(null);
            addTarget(result, request, broadcastId, customer, primary);
        }
        return new ArrayList<>(result.values());
    }

    private void addTarget(Map<String, CrmMarketingBroadcastRecipientDO> result, CrmMarketingBroadcastSaveReqVO request,
                           Long broadcastId, CrmCustomerDO customer, CrmContactDO contact) {
        String mobile = contact != null && contact.getMobile() != null ? contact.getMobile() : customer.getMobile();
        String email = contact != null && contact.getEmail() != null ? contact.getEmail() : customer.getEmail();
        List<Integer> channels = request.getChannel() == 3 ? List.of(1, 2) : List.of(request.getChannel());
        for (Integer channel : channels) {
            CrmMarketingBroadcastRecipientDO recipient = new CrmMarketingBroadcastRecipientDO()
                    .setBroadcastId(broadcastId).setCustomerId(customer.getId()).setContactId(contact == null ? null : contact.getId())
                    .setChannel(channel).setMobile(mobile).setEmail(email).setStatus(CrmMarketingRecipientStatusEnum.PENDING.getStatus())
                    .setAttemptCount(0);
            String reason = channel == 1 && (mobile == null || mobile.isBlank()) ? "手机号为空"
                    : channel == 2 && (email == null || email.isBlank()) ? "邮箱为空" : null;
            CrmMarketingConsentDO consent = resolveConsent(customer.getId(), contact == null ? null : contact.getId(), channel);
            if (reason != null) recipient.setStatus(CrmMarketingRecipientStatusEnum.SUPPRESSED.getStatus()).setSuppressedReason(reason);
            else if (consent == null || CrmMarketingConsentStatusEnum.OPTED_OUT.getStatus().equals(consent.getStatus()))
                recipient.setStatus(CrmMarketingRecipientStatusEnum.SUPPRESSED.getStatus()).setSuppressedReason("未获得该渠道同意或已退订");
            result.put(customer.getId() + ":" + (contact == null ? 0 : contact.getId()) + ":" + channel, recipient);
        }
    }

    void sendRecipients(CrmMarketingBroadcastDO broadcast) {
        List<CrmMarketingBroadcastRecipientDO> recipients;
        while (true) {
            recipients = recipientMapper.selectPending(broadcast.getId(), properties.getBatchSize());
            if (recipients.isEmpty()) break;
            int claimed = 0;
            for (CrmMarketingBroadcastRecipientDO recipient : recipients) {
                if (recipientMapper.claimForSending(recipient.getId(), LocalDateTime.now()) == 0) continue;
                claimed++;
                try {
                    if ("record-only".equals(properties.getProviderMode())) {
                        recipient.setStatus(CrmMarketingRecipientStatusEnum.RECORDED.getStatus()).setSentAt(LocalDateTime.now());
                    } else {
                        Map<String, Object> params = parseParams(broadcast.getTemplateParams());
                        Long logId;
                        if (recipient.getChannel() == 1) {
                            SmsSendSingleToUserReqDTO req = new SmsSendSingleToUserReqDTO();
                            req.setMobile(recipient.getMobile()); req.setTemplateCode(broadcast.getSmsTemplateCode()); req.setTemplateParams(params);
                            logId = smsSendApi.sendSingleSmsToAdmin(req);
                        } else {
                            MailSendSingleToUserReqDTO req = new MailSendSingleToUserReqDTO();
                            req.setToMails(List.of(recipient.getEmail())); req.setTemplateCode(broadcast.getMailTemplateCode()); req.setTemplateParams(params);
                            logId = mailSendApi.sendSingleMailToAdmin(req);
                        }
                        if (logId == null) throw new IllegalStateException("消息提供商未返回发送日志编号");
                        recipient.setProviderLogId(logId).setStatus(CrmMarketingRecipientStatusEnum.SENT.getStatus()).setSentAt(LocalDateTime.now());
                    }
                } catch (RuntimeException ex) {
                    recipient.setStatus(CrmMarketingRecipientStatusEnum.FAILED.getStatus()).setFailureReason(ex.getMessage());
                }
                recipientMapper.updateById(recipient);
            }
            if (claimed == 0) return;
        }
        List<CrmMarketingBroadcastRecipientDO> all = recipientMapper.selectList(CrmMarketingBroadcastRecipientDO::getBroadcastId, broadcast.getId());
        boolean inFlight = all.stream().anyMatch(item -> CrmMarketingRecipientStatusEnum.PENDING.getStatus().equals(item.getStatus())
                || CrmMarketingRecipientStatusEnum.SENDING.getStatus().equals(item.getStatus()));
        if (inFlight) return;
        long sent = all.stream().filter(item -> item.getStatus().equals(CrmMarketingRecipientStatusEnum.SENT.getStatus())
                || item.getStatus().equals(CrmMarketingRecipientStatusEnum.RECORDED.getStatus())).count();
        long failed = all.stream().filter(item -> item.getStatus().equals(CrmMarketingRecipientStatusEnum.FAILED.getStatus())).count();
        broadcast.setSentCount((int) sent).setFailedCount((int) failed)
                .setStatus(failed > 0 ? CrmMarketingBroadcastStatusEnum.PARTIAL_FAILED.getStatus() : CrmMarketingBroadcastStatusEnum.SENT.getStatus())
                .setSentAt(LocalDateTime.now());
        broadcastMapper.updateById(broadcast);
    }

    private void enforceQuota(Long broadcastId) {
        long monthCount = recipientMapper.selectCount(new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<CrmMarketingBroadcastRecipientDO>()
                .ge(CrmMarketingBroadcastRecipientDO::getCreateTime, YearMonth.now().atDay(1).atStartOfDay())
                .in(CrmMarketingBroadcastRecipientDO::getStatus, CrmMarketingRecipientStatusEnum.SENT.getStatus(), CrmMarketingRecipientStatusEnum.RECORDED.getStatus()));
        long pending = recipientMapper.selectCount(new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<CrmMarketingBroadcastRecipientDO>()
                .eq(CrmMarketingBroadcastRecipientDO::getBroadcastId, broadcastId)
                .eq(CrmMarketingBroadcastRecipientDO::getStatus, CrmMarketingRecipientStatusEnum.PENDING.getStatus()));
        if (monthCount + pending > properties.getMonthlyRecipientLimit()) throw exception(MARKETING_QUOTA_EXCEEDED);
        LocalDateTime today = LocalDate.now().atStartOfDay();
        for (CrmMarketingBroadcastRecipientDO recipient : recipientMapper.selectPending(broadcastId, properties.getMaxBatchSize())) {
            long daily = recipientMapper.selectCount(new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<CrmMarketingBroadcastRecipientDO>()
                    .eq(CrmMarketingBroadcastRecipientDO::getCustomerId, recipient.getCustomerId())
                    .eq(CrmMarketingBroadcastRecipientDO::getContactId, recipient.getContactId())
                    .eq(CrmMarketingBroadcastRecipientDO::getChannel, recipient.getChannel())
                    .ge(CrmMarketingBroadcastRecipientDO::getSentAt, today)
                    .in(CrmMarketingBroadcastRecipientDO::getStatus, CrmMarketingRecipientStatusEnum.SENT.getStatus(), CrmMarketingRecipientStatusEnum.RECORDED.getStatus()));
            if (daily >= properties.getPerRecipientDailyLimit()) throw exception(MARKETING_QUOTA_EXCEEDED);
        }
    }

    private void validateChannel(Integer channel, String smsTemplate, String mailTemplate) {
        if (channel == null || channel < 1 || channel > 3) throw exception(MARKETING_CHANNEL_INVALID);
        if ((channel == 1 || channel == 3) && (smsTemplate == null || smsTemplate.isBlank())) throw exception(MARKETING_TEMPLATE_REQUIRED);
        if ((channel == 2 || channel == 3) && (mailTemplate == null || mailTemplate.isBlank())) throw exception(MARKETING_TEMPLATE_REQUIRED);
    }

    private void checkReadable(CrmCustomerDO customer, Long userId) {
        if (authorizationService.isCrmAdmin(userId)) return;
        if (!authorizationService.resolveOwnerReadScope(userId).allows(customer.getOwnerUserId())) throw exception(MARKETING_PERMISSION_DENIED);
    }

    CrmMarketingConsentDO resolveConsent(Long customerId, Long contactId, Integer channel) {
        CrmMarketingConsentDO consent = consentMapper.selectTarget(customerId, contactId, channel);
        return consent != null || contactId == null ? consent : consentMapper.selectTarget(customerId, null, channel);
    }

    private CrmMarketingBroadcastDO requireBroadcast(Long id) {
        CrmMarketingBroadcastDO row = broadcastMapper.selectById(id);
        if (row == null) throw exception(MARKETING_BROADCAST_NOT_EXISTS);
        return row;
    }

    private Map<String, Object> parseParams(String raw) {
        if (raw == null || raw.isBlank()) return Map.of();
        try { return objectMapper.readValue(raw, new TypeReference<>() {}); }
        catch (Exception ex) { throw exception(MARKETING_TEMPLATE_REQUIRED); }
    }

    public Long parseCreatorUserId(String creator) {
        try { return creator == null ? null : Long.valueOf(creator); } catch (NumberFormatException ex) { return null; }
    }

    private void requireCreatorOrAdmin(CrmMarketingBroadcastDO row, Long userId) {
        if (!Objects.equals(parseCreatorUserId(row.getCreator()), userId)
                && !authorizationService.isCrmAdmin(userId)) {
            throw exception(MARKETING_PERMISSION_DENIED);
        }
    }
}
