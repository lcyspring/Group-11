package com.meession.etm.module.crm.dal.dataobject.marketing;

import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.meession.etm.framework.mybatis.core.dataobject.BaseDO;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

@TableName("crm_marketing_broadcast")
@KeySequence("crm_marketing_broadcast_seq")
@Data
@EqualsAndHashCode(callSuper = true)
public class CrmMarketingBroadcastDO extends BaseDO {
    @TableId private Long id;
    private Long campaignId;
    private String name;
    private Integer channel;
    private String smsTemplateCode;
    private String mailTemplateCode;
    private String templateParams;
    private Integer status;
    private Integer totalCount;
    private Integer validCount;
    private Integer suppressedCount;
    private Integer sentCount;
    private Integer failedCount;
    private Long reviewerUserId;
    private LocalDateTime reviewedAt;
    private String reviewComment;
    private LocalDateTime scheduledAt;
    private LocalDateTime sentAt;
}
