package com.meession.etm.module.marketing.dal.dataobject.roi;

import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.meession.etm.framework.tenant.core.db.TenantBaseDO;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 营销活动 ROI 数据 DO
 * <p>
 * 用于沉淀活动维度的成本、收入与转化数据，供 ROI 分析接口聚合使用。
 * 后续 CRM 链路补齐 campaignId 归因后，可由定时任务或事件回写该表。
 *
 * @author MITEDTSM
 */
@TableName("marketing_campaign_roi")
@KeySequence("marketing_campaign_roi_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MarketingCampaignRoiDO extends TenantBaseDO {

    @TableId
    private Long id;

    /**
     * 统计日期
     */
    private LocalDate statDate;

    /**
     * 营销活动编号
     */
    private Long campaignId;

    /**
     * 渠道：SMS / MAIL / OTHER
     */
    private String channel;

    /**
     * 成本金额
     */
    private BigDecimal costAmount;

    /**
     * 收入金额
     */
    private BigDecimal revenueAmount;

    /**
     * 线索数量
     */
    private Integer leadCount;

    /**
     * 客户数量
     */
    private Integer customerCount;

    /**
     * 商机数量
     */
    private Integer opportunityCount;

    /**
     * 成交数量
     */
    private Integer dealCount;

    /**
     * 备注
     */
    private String remark;

}
