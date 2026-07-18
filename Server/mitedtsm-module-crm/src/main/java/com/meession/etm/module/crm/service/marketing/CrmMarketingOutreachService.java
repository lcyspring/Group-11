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
import com.meession.etm.module.system.api.mail.dto.MailSendStatusRespDTO;
import com.meession.etm.module.system.api.sms.SmsSendApi;
import com.meession.etm.module.system.api.sms.dto.SmsSendStatusRespDTO;
import com.meession.etm.module.system.api.sms.dto.send.SmsSendSingleToUserReqDTO;
import com.meession.etm.module.system.enums.mail.MailSendStatusEnum;
import com.meession.etm.module.system.enums.sms.SmsReceiveStatusEnum;
import com.meession.etm.module.system.enums.sms.SmsSendStatusEnum;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.net.URI;
import java.security.SecureRandom;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;

import static com.meession.etm.framework.common.exception.util.ServiceExceptionUtil.exception;
import static com.meession.etm.module.crm.enums.ErrorCodeConstants.*;

@Service
public class CrmMarketingOutreachService {
    private static final SecureRandom TRACKING_RANDOM = new SecureRandom();
    @Resource private CrmMarketingBroadcastMapper broadcastMapper;
    @Resource private CrmMarketingBroadcastRecipientMapper recipientMapper;
    @Resource private CrmMarketingConsentMapper consentMapper;
    @Resource private CrmMarketingLinkMapper linkMapper;
    @Resource private CrmMarketingLinkRecipientMapper linkRecipientMapper;
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
        validateLinks(request.getLinks());
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
        replaceLinks(row.getId(), request.getLinks());
        row.setTotalCount(recipients.size());
        row.setValidCount((int) recipients.stream().filter(item -> item.getStatus().equals(CrmMarketingRecipientStatusEnum.PENDING.getStatus())).count());
        row.setSuppressedCount(recipients.size() - row.getValidCount());
        row.setSentCount(0).setFailedCount(0);
        broadcastMapper.updateById(row);
        return row.getId();
    }

    public PageResult<CrmMarketingBroadcastDO> getBroadcastPage(CrmMarketingBroadcastPageReqVO request,
                                                                 Long userId, boolean privilegedReader) {
        boolean readAll = privilegedReader || authorizationService.isCrmAdmin(userId);
        return broadcastMapper.selectPage(request, readAll, String.valueOf(userId));
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

    public CrmMarketingBroadcastDO getBroadcast(Long id, Long userId, boolean privilegedReader) {
        CrmMarketingBroadcastDO row = requireBroadcast(id);
        requireReadable(row, userId, privilegedReader);
        return row;
    }

    public List<CrmMarketingBroadcastRecipientDO> getBroadcastRecipients(Long id) {
        requireBroadcast(id);
        return recipientMapper.selectList(CrmMarketingBroadcastRecipientDO::getBroadcastId, id);
    }

    public List<CrmMarketingBroadcastRecipientDO> getBroadcastRecipients(Long id, Long userId,
                                                                          boolean privilegedReader) {
        requireReadable(requireBroadcast(id), userId, privilegedReader);
        return recipientMapper.selectList(CrmMarketingBroadcastRecipientDO::getBroadcastId, id);
    }

    public List<CrmMarketingLinkDO> getBroadcastLinks(Long id, Long userId, boolean privilegedReader) {
        requireReadable(requireBroadcast(id), userId, privilegedReader);
        return linkMapper.selectByBroadcastId(id);
    }

    @Transactional(rollbackFor = Exception.class)
    public int refreshDraftRecipients(Long id, Long userId) {
        CrmMarketingBroadcastDO row = requireBroadcast(id);
        requireCreatorOrAdmin(row, userId);
        if (!List.of(CrmMarketingBroadcastStatusEnum.DRAFT.getStatus(),
                CrmMarketingBroadcastStatusEnum.REJECTED.getStatus()).contains(row.getStatus())) {
            throw exception(MARKETING_BROADCAST_STATUS_INVALID);
        }
        List<CrmMarketingBroadcastRecipientDO> oldRecipients = recipientMapper.selectList(
                CrmMarketingBroadcastRecipientDO::getBroadcastId, id);
        CrmMarketingBroadcastSaveReqVO request = BeanUtils.toBean(row, CrmMarketingBroadcastSaveReqVO.class).setId(id);
        Map<String, CrmMarketingBroadcastRecipientDO> rebuilt = new LinkedHashMap<>();
        for (CrmMarketingBroadcastRecipientDO oldRecipient : oldRecipients) {
            CrmCustomerDO customer = customerMapper.selectById(oldRecipient.getCustomerId());
            if (customer == null) {
                continue;
            }
            checkReadable(customer, userId);
            CrmContactDO contact = oldRecipient.getContactId() == null
                    ? null : contactMapper.selectById(oldRecipient.getContactId());
            if (contact != null && !Objects.equals(contact.getCustomerId(), customer.getId())) {
                contact = null;
            }
            addTarget(rebuilt, request, id, customer, contact);
        }
        recipientMapper.delete(CrmMarketingBroadcastRecipientDO::getBroadcastId, id);
        List<CrmMarketingBroadcastRecipientDO> recipients = new ArrayList<>(rebuilt.values());
        recipients.forEach(recipientMapper::insert);
        int validCount = (int) recipients.stream().filter(item ->
                CrmMarketingRecipientStatusEnum.PENDING.getStatus().equals(item.getStatus())).count();
        broadcastMapper.updateById(new CrmMarketingBroadcastDO().setId(id)
                .setTotalCount(recipients.size()).setValidCount(validCount)
                .setSuppressedCount(recipients.size() - validCount));
        return validCount;
    }

    @Transactional(rollbackFor = Exception.class)
    public void deleteBroadcast(Long id, Long userId) {
        CrmMarketingBroadcastDO row = requireBroadcast(id);
        requireCreatorOrAdmin(row, userId);
        if (broadcastMapper.deleteDraft(id, CrmMarketingBroadcastStatusEnum.DRAFT.getStatus()) == 0) {
            throw exception(MARKETING_BROADCAST_STATUS_INVALID);
        }
        recipientMapper.delete(CrmMarketingBroadcastRecipientDO::getBroadcastId, id);
        deleteLinks(id);
    }

    public PageResult<CrmMarketingBroadcastRecipientDO> getRecipientPage(CrmMarketingRecipientPageReqVO request) {
        return recipientMapper.selectPage(request);
    }

    public PageResult<CrmMarketingBroadcastRecipientDO> getRecipientPage(CrmMarketingRecipientPageReqVO request,
                                                                         Long userId, boolean privilegedReader) {
        requireReadable(requireBroadcast(request.getBroadcastId()), userId, privilegedReader);
        return recipientMapper.selectPage(request);
    }

    public CrmMarketingDeliverySummaryRespVO getDeliverySummary(Long id, Long userId, boolean privilegedReader) {
        requireReadable(requireBroadcast(id), userId, privilegedReader);
        List<CrmMarketingLinkDO> links = linkMapper.selectByBroadcastId(id);
        return buildDeliverySummary(id, recipientMapper.selectList(
                CrmMarketingBroadcastRecipientDO::getBroadcastId, id), links,
                linkRecipientMapper.selectByLinkIds(links.stream().map(CrmMarketingLinkDO::getId).toList()));
    }

    @Transactional(rollbackFor = Exception.class)
    public int syncDeliveryResults(Long id, Long userId, boolean privilegedReader) {
        requireReadable(requireBroadcast(id), userId, privilegedReader);
        return syncDeliveryResults(recipientMapper.selectList(
                CrmMarketingBroadcastRecipientDO::getBroadcastId, id));
    }

    @Transactional(rollbackFor = Exception.class)
    public int syncPendingDeliveryResults() {
        return syncDeliveryResults(recipientMapper.selectPendingDeliveryResults(properties.getDeliverySyncBatchSize()));
    }

    public void recordMailOpen(String token) {
        if (!properties.isTrackingEnabled() || token == null || !token.matches("[0-9a-f]{32}")) {
            return;
        }
        CrmMarketingBroadcastRecipientDO recipient = recipientMapper.selectByTrackingToken(token);
        if (recipient == null || !Integer.valueOf(2).equals(recipient.getChannel())) {
            return;
        }
        recipientMapper.markOpened(token, LocalDateTime.now());
    }

    public Optional<String> recordLinkClick(String token) {
        if (!properties.isClickTrackingEnabled() || token == null || !token.matches("[A-Za-z0-9_-]{48}")) {
            return Optional.empty();
        }
        CrmMarketingLinkRecipientDO fact = linkRecipientMapper.selectByTrackingToken(token);
        if (fact == null) return Optional.empty();
        CrmMarketingBroadcastRecipientDO recipient = recipientMapper.selectByIdIgnoringTenant(fact.getRecipientId());
        if (recipient == null || !CrmMarketingRecipientStatusEnum.SENT.getStatus().equals(recipient.getStatus())) {
            return Optional.empty();
        }
        CrmMarketingLinkDO link = linkMapper.selectByIdIgnoringTenant(fact.getLinkId());
        if (link == null || !isAllowedTargetUrl(link.getTargetUrl())) return Optional.empty();
        if (linkRecipientMapper.recordClick(fact.getId(), LocalDateTime.now()) == 0) return Optional.empty();
        return Optional.of(link.getTargetUrl());
    }

    public void submitReview(Long id, Long userId) {
        CrmMarketingBroadcastDO row = requireBroadcast(id);
        requireCreatorOrAdmin(row, userId);
        if (row.getValidCount() == null || row.getValidCount() <= 0) {
            throw exception(MARKETING_RECIPIENT_NONE_SENDABLE);
        }
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
        List<CrmMarketingLinkDO> links = linkMapper.selectByBroadcastId(broadcast.getId());
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
                        recipient.setStatus(CrmMarketingRecipientStatusEnum.RECORDED.getStatus()).setSentAt(LocalDateTime.now())
                                .setDeliveryStatus(CrmMarketingDeliveryStatusEnum.UNKNOWN.getStatus());
                    } else {
                        Map<String, Object> params = new HashMap<>(parseParams(broadcast.getTemplateParams()));
                        addTrackedLinkParams(recipient, links, params);
                        Long logId;
                        if (recipient.getChannel() == 1) {
                            SmsSendSingleToUserReqDTO req = new SmsSendSingleToUserReqDTO();
                            req.setMobile(recipient.getMobile()); req.setTemplateCode(broadcast.getSmsTemplateCode()); req.setTemplateParams(params);
                            logId = smsSendApi.sendSingleSmsToAdmin(req);
                        } else {
                            if (properties.isTrackingEnabled()) {
                                String trackingToken = UUID.randomUUID().toString().replace("-", "");
                                recipient.setTrackingToken(trackingToken);
                                params.put("__trackingPixelUrl", buildTrackingUrl(trackingToken));
                            }
                            MailSendSingleToUserReqDTO req = new MailSendSingleToUserReqDTO();
                            req.setToMails(List.of(recipient.getEmail())); req.setTemplateCode(broadcast.getMailTemplateCode()); req.setTemplateParams(params);
                            logId = mailSendApi.sendSingleMailToAdmin(req);
                        }
                        if (logId == null) throw new IllegalStateException("消息提供商未返回发送日志编号");
                        recipient.setProviderLogId(logId).setStatus(CrmMarketingRecipientStatusEnum.SENT.getStatus())
                                .setSentAt(LocalDateTime.now())
                                .setDeliveryStatus(CrmMarketingDeliveryStatusEnum.PROVIDER_PENDING.getStatus());
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

    private int syncDeliveryResults(List<CrmMarketingBroadcastRecipientDO> recipients) {
        int changed = 0;
        for (CrmMarketingBroadcastRecipientDO recipient : recipients) {
            if (recipient.getProviderLogId() == null
                    || !Objects.equals(CrmMarketingDeliveryStatusEnum.PROVIDER_PENDING.getStatus(),
                            recipient.getDeliveryStatus())) {
                continue;
            }
            CrmMarketingBroadcastRecipientDO update = resolveProviderResult(recipient);
            if (update != null) {
                recipientMapper.updateById(update);
                changed++;
            }
        }
        return changed;
    }

    private CrmMarketingBroadcastRecipientDO resolveProviderResult(CrmMarketingBroadcastRecipientDO recipient) {
        CrmMarketingBroadcastRecipientDO update = new CrmMarketingBroadcastRecipientDO().setId(recipient.getId());
        if (Integer.valueOf(1).equals(recipient.getChannel())) {
            SmsSendStatusRespDTO status = smsSendApi.getSmsSendStatus(recipient.getProviderLogId());
            if (status == null || Objects.equals(SmsSendStatusEnum.INIT.getStatus(), status.getSendStatus())) return null;
            if (Objects.equals(SmsSendStatusEnum.FAILURE.getStatus(), status.getSendStatus())
                    || Objects.equals(SmsSendStatusEnum.IGNORE.getStatus(), status.getSendStatus())) {
                return update.setDeliveryStatus(CrmMarketingDeliveryStatusEnum.FAILED.getStatus())
                        .setFailureReason(Optional.ofNullable(status.getSendMessage()).orElse("短信提供商发送失败"));
            }
            if (Objects.equals(SmsReceiveStatusEnum.SUCCESS.getStatus(), status.getReceiveStatus())) {
                return update.setDeliveryStatus(CrmMarketingDeliveryStatusEnum.DELIVERED.getStatus())
                        .setDeliveredAt(status.getReceiveTime());
            }
            if (Objects.equals(SmsReceiveStatusEnum.FAILURE.getStatus(), status.getReceiveStatus())) {
                return update.setDeliveryStatus(CrmMarketingDeliveryStatusEnum.FAILED.getStatus())
                        .setFailureReason(Optional.ofNullable(status.getReceiveMessage()).orElse("短信送达失败"));
            }
            return null;
        }
        MailSendStatusRespDTO status = mailSendApi.getMailSendStatus(recipient.getProviderLogId());
        if (status == null || Objects.equals(MailSendStatusEnum.INIT.getStatus(), status.getSendStatus())) return null;
        if (Objects.equals(MailSendStatusEnum.SUCCESS.getStatus(), status.getSendStatus())) {
            return update.setDeliveryStatus(CrmMarketingDeliveryStatusEnum.ACCEPTED.getStatus())
                    .setDeliveredAt(status.getSendTime());
        }
        return update.setDeliveryStatus(CrmMarketingDeliveryStatusEnum.FAILED.getStatus())
                .setFailureReason(Optional.ofNullable(status.getSendException()).orElse("邮件提供商发送失败"));
    }

    private CrmMarketingDeliverySummaryRespVO buildDeliverySummary(Long broadcastId,
                                                                    List<CrmMarketingBroadcastRecipientDO> recipients,
                                                                    List<CrmMarketingLinkDO> links,
                                                                    List<CrmMarketingLinkRecipientDO> clickFacts) {
        int smsSent = count(recipients, 1, null, false);
        int smsDelivered = count(recipients, 1, CrmMarketingDeliveryStatusEnum.DELIVERED.getStatus(), false);
        int smsFailed = count(recipients, 1, CrmMarketingDeliveryStatusEnum.FAILED.getStatus(), false);
        int emailSent = count(recipients, 2, null, false);
        int emailAccepted = count(recipients, 2, CrmMarketingDeliveryStatusEnum.ACCEPTED.getStatus(), false);
        int emailFailed = count(recipients, 2, CrmMarketingDeliveryStatusEnum.FAILED.getStatus(), false);
        int emailOpened = count(recipients, 2, null, true);
        int pending = (int) recipients.stream().filter(item -> Objects.equals(
                CrmMarketingDeliveryStatusEnum.PROVIDER_PENDING.getStatus(), item.getDeliveryStatus())).count();
        int unknown = (int) recipients.stream().filter(item -> item.getDeliveryStatus() == null
                || Objects.equals(CrmMarketingDeliveryStatusEnum.UNKNOWN.getStatus(), item.getDeliveryStatus())).count();
        int trackedRecipients = (int) clickFacts.stream().map(CrmMarketingLinkRecipientDO::getRecipientId).distinct().count();
        int uniqueClicks = (int) clickFacts.stream().filter(item -> item.getFirstClickedAt() != null)
                .map(CrmMarketingLinkRecipientDO::getRecipientId).distinct().count();
        int totalClicks = clickFacts.stream().map(CrmMarketingLinkRecipientDO::getClickCount)
                .filter(Objects::nonNull).mapToInt(Integer::intValue).sum();
        List<CrmMarketingLinkSummaryRespVO> linkSummaries = links.stream().map(link -> {
            List<CrmMarketingLinkRecipientDO> facts = clickFacts.stream()
                    .filter(item -> Objects.equals(link.getId(), item.getLinkId())).toList();
            int linkUniqueClicks = (int) facts.stream().filter(item -> item.getFirstClickedAt() != null).count();
            int linkTotalClicks = facts.stream().map(CrmMarketingLinkRecipientDO::getClickCount)
                    .filter(Objects::nonNull).mapToInt(Integer::intValue).sum();
            return new CrmMarketingLinkSummaryRespVO().setLinkId(link.getId()).setCode(link.getCode())
                    .setName(link.getName()).setTargetUrl(link.getTargetUrl())
                    .setTrackedRecipientCount(facts.size()).setUniqueClickCount(linkUniqueClicks)
                    .setTotalClickCount(linkTotalClicks).setUniqueClickRate(rate(linkUniqueClicks, facts.size()));
        }).toList();
        return new CrmMarketingDeliverySummaryRespVO().setBroadcastId(broadcastId)
                .setSmsSentCount(smsSent).setSmsDeliveredCount(smsDelivered).setSmsFailedCount(smsFailed)
                .setSmsDeliveryRate(rate(smsDelivered, smsSent))
                .setEmailSentCount(emailSent).setEmailAcceptedCount(emailAccepted).setEmailFailedCount(emailFailed)
                .setEmailOpenedCount(emailOpened).setEmailOpenRate(rate(emailOpened, emailAccepted))
                .setProviderPendingCount(pending).setUnknownCount(unknown)
                .setTrackedRecipientCount(trackedRecipients).setUniqueClickCount(uniqueClicks)
                .setTotalClickCount(totalClicks).setUniqueClickRate(rate(uniqueClicks, trackedRecipients))
                .setLinks(linkSummaries);
    }

    private int count(List<CrmMarketingBroadcastRecipientDO> recipients, int channel,
                      Integer deliveryStatus, boolean opened) {
        return (int) recipients.stream().filter(item -> Integer.valueOf(channel).equals(item.getChannel()))
                .filter(item -> item.getProviderLogId() != null)
                .filter(item -> deliveryStatus == null || deliveryStatus.equals(item.getDeliveryStatus()))
                .filter(item -> !opened || item.getOpenedAt() != null).count();
    }

    private BigDecimal rate(int numerator, int denominator) {
        if (denominator == 0) return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        return BigDecimal.valueOf(numerator).multiply(BigDecimal.valueOf(100))
                .divide(BigDecimal.valueOf(denominator), 2, RoundingMode.HALF_UP);
    }

    private String buildTrackingUrl(String token) {
        String baseUrl = properties.getPublicBaseUrl().replaceAll("/+$", "");
        return baseUrl + "/app-api/crm/marketing/open/" + token + ".gif";
    }

    private String buildClickUrl(String token) {
        String baseUrl = properties.getPublicBaseUrl().replaceAll("/+$", "");
        return baseUrl + "/app-api/crm/marketing/click/" + token;
    }

    void validateLinks(List<CrmMarketingLinkSaveReqVO> links) {
        List<CrmMarketingLinkSaveReqVO> values = Optional.ofNullable(links).orElse(List.of());
        if (values.isEmpty()) return;
        if (!properties.isClickTrackingEnabled()) throw exception(MARKETING_LINK_TRACKING_DISABLED);
        if (values.size() > properties.getMaxLinksPerBroadcast()) throw exception(MARKETING_LINK_INVALID);
        Set<String> codes = new HashSet<>();
        for (CrmMarketingLinkSaveReqVO link : values) {
            if (link == null || link.getCode() == null || link.getName() == null
                    || !link.getCode().matches("^[A-Za-z][A-Za-z0-9_]{0,31}$")
                    || !codes.add(link.getCode().toLowerCase(Locale.ROOT))
                    || !isAllowedTargetUrl(link.getTargetUrl())) {
                throw exception(MARKETING_LINK_INVALID);
            }
        }
    }

    boolean isAllowedTargetUrl(String targetUrl) {
        try {
            URI uri = URI.create(targetUrl);
            if (uri.getUserInfo() != null || uri.getHost() == null
                    || !("http".equalsIgnoreCase(uri.getScheme()) || "https".equalsIgnoreCase(uri.getScheme()))) {
                return false;
            }
            String host = uri.getHost().toLowerCase(Locale.ROOT);
            return Arrays.stream(properties.getClickAllowedHosts().split(","))
                    .map(String::trim).filter(value -> !value.isEmpty()).map(value -> value.toLowerCase(Locale.ROOT))
                    .anyMatch(allowed -> allowed.startsWith("*.")
                            ? host.endsWith(allowed.substring(1)) && host.length() > allowed.length() - 1
                            : host.equals(allowed));
        } catch (IllegalArgumentException ignored) {
            return false;
        }
    }

    private void replaceLinks(Long broadcastId, List<CrmMarketingLinkSaveReqVO> links) {
        deleteLinks(broadcastId);
        for (CrmMarketingLinkSaveReqVO request : Optional.ofNullable(links).orElse(List.of())) {
            linkMapper.insert(BeanUtils.toBean(request, CrmMarketingLinkDO.class).setBroadcastId(broadcastId));
        }
    }

    private void deleteLinks(Long broadcastId) {
        List<CrmMarketingLinkDO> links = linkMapper.selectByBroadcastId(broadcastId);
        if (!links.isEmpty()) {
            linkRecipientMapper.deletePhysicalByLinkIds(links.stream().map(CrmMarketingLinkDO::getId).toList());
        }
        linkMapper.deletePhysicalByBroadcast(broadcastId);
    }

    private void addTrackedLinkParams(CrmMarketingBroadcastRecipientDO recipient, List<CrmMarketingLinkDO> links,
                                      Map<String, Object> params) {
        if (!properties.isClickTrackingEnabled() || links.isEmpty()) return;
        for (CrmMarketingLinkDO link : links) {
            CrmMarketingLinkRecipientDO fact = linkRecipientMapper.selectByLinkAndRecipient(link.getId(), recipient.getId());
            if (fact == null) {
                byte[] random = new byte[36];
                TRACKING_RANDOM.nextBytes(random);
                fact = new CrmMarketingLinkRecipientDO().setLinkId(link.getId()).setRecipientId(recipient.getId())
                        .setTrackingToken(Base64.getUrlEncoder().withoutPadding().encodeToString(random)).setClickCount(0);
                linkRecipientMapper.insert(fact);
            }
            params.put(link.getCode(), buildClickUrl(fact.getTrackingToken()));
        }
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

    private void requireReadable(CrmMarketingBroadcastDO row, Long userId, boolean privilegedReader) {
        if (privilegedReader || authorizationService.isCrmAdmin(userId)
                || Objects.equals(parseCreatorUserId(row.getCreator()), userId)) {
            return;
        }
        throw exception(MARKETING_PERMISSION_DENIED);
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
