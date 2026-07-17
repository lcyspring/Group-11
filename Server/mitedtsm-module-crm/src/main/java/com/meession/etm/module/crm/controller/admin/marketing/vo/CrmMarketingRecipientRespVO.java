package com.meession.etm.module.crm.controller.admin.marketing.vo;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class CrmMarketingRecipientRespVO {
    private Long id; private Long broadcastId; private Long customerId; private Long contactId;
    private Integer channel; private String mobile; private String email; private Integer status;
    private String suppressedReason; private Long providerLogId; private String failureReason;
    private Integer attemptCount; private LocalDateTime sentAt; private LocalDateTime lastAttemptAt;
    private Integer deliveryStatus; private LocalDateTime deliveredAt; private LocalDateTime openedAt;
}
