package com.meession.etm.module.crm.dal.dataobject.marketing;

import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.meession.etm.framework.mybatis.core.dataobject.BaseDO;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@TableName("crm_marketing_campaign_relation")
@KeySequence("crm_marketing_campaign_relation_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class CrmMarketingCampaignRelationDO extends BaseDO {
    @TableId
    private Long id;
    private Long campaignId;
    private Integer bizType;
    private Long bizId;
}
