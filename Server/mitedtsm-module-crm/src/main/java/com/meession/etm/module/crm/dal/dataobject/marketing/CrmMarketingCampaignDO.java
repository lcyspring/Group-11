package com.meession.etm.module.crm.dal.dataobject.marketing;

import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.meession.etm.framework.mybatis.core.dataobject.BaseDO;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@TableName("crm_marketing_campaign")
@KeySequence("crm_marketing_campaign_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class CrmMarketingCampaignDO extends BaseDO {
    @TableId
    private Long id;
    private String code;
    private String name;
    private Integer status;
    private Long ownerUserId;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private BigDecimal budgetAmount;
    private BigDecimal actualCostAmount;
    private Integer targetLeadCount;
    private Integer targetCustomerCount;
    private String description;
    private String summary;
    private LocalDateTime lockedTime;
    private LocalDateTime terminatedTime;
    private LocalDateTime completedTime;
}
