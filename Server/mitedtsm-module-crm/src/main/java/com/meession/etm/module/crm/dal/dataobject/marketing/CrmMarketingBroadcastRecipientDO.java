package com.meession.etm.module.crm.dal.dataobject.marketing;

import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.meession.etm.framework.mybatis.core.dataobject.BaseDO;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

@TableName("crm_marketing_broadcast_recipient")
@KeySequence("crm_marketing_broadcast_recipient_seq")
@Data
@EqualsAndHashCode(callSuper = true)
public class CrmMarketingBroadcastRecipientDO extends BaseDO {
    @TableId private Long id;
    private Long broadcastId;
    private Long customerId;
    private String customerName;
    private Long contactId;
    private String contactName;
    private Integer channel;
    private String mobile;
    private String email;
    private Integer status;
    private String suppressedReason;
    private Long providerLogId;
    private String failureReason;
    private Integer attemptCount;
    private LocalDateTime sentAt;
    private LocalDateTime lastAttemptAt;
    private Integer deliveryStatus;
    private LocalDateTime deliveredAt;
    private LocalDateTime openedAt;
    private String trackingToken;
}
