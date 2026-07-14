package com.meession.etm.module.crm.dal.dataobject.marketing;

import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.meession.etm.framework.mybatis.core.dataobject.BaseDO;
import com.meession.etm.module.crm.enums.marketing.CrmMarketingCampaignStatusEnum;
import com.meession.etm.module.crm.enums.marketing.CrmMarketingCampaignTypeEnum;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * CRM 营销活动 DO
 *
 * @author mitedtsm
 */
@TableName("crm_marketing_campaign")
@KeySequence("crm_marketing_campaign_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CrmMarketingCampaignDO extends BaseDO {

    /**
     * 编号
     */
    @TableId
    private Long id;

    /**
     * 活动名称
     */
    private String name;

    /**
     * 活动类型
     *
     * 枚举 {@link CrmMarketingCampaignTypeEnum}
     */
    private Integer type;

    /**
     * 活动状态
     *
     * 枚举 {@link CrmMarketingCampaignStatusEnum}
     */
    private Integer status;

    /**
     * 活动开始时间
     */
    private LocalDateTime startTime;

    /**
     * 活动结束时间
     */
    private LocalDateTime endTime;

    /**
     * 目标客户编号列表
     */
    private String targetCustomerIds;

    /**
     * 活动描述
     */
    private String description;

    /**
     * 活动预算，单位：元
     */
    private BigDecimal budget;

    /**
     * 实际花费，单位：元
     */
    private BigDecimal actualCost;

    /**
     * 关联的模板编号
     */
    private Long templateId;

}
