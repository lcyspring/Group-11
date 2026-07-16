package com.meession.etm.module.marketing.dal.dataobject.campaign;

import com.meession.etm.framework.mybatis.core.dataobject.BaseDO;
import com.meession.etm.framework.tenant.core.aop.TenantIgnore;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.*;

import java.time.LocalDateTime;

/**
 * 营销活动 DO
 *
 * @author MITEDTSM
 */
@TableName("marketing_campaign")
@KeySequence("marketing_campaign_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TenantIgnore
public class MarketingCampaignDO extends BaseDO {

    /**
     * 编号，主键自增
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
     * 枚举 {@link com.meession.etm.module.marketing.enums.CampaignTypeEnum}
     */
    private Integer type;

    /**
     * 活动状态
     *
     * 枚举 {@link com.meession.etm.module.marketing.enums.CampaignStatusEnum}
     */
    private Integer status;

    /**
     * 关联模板ID（短信模板或邮件模板）
     */
    private Long templateId;

    /**
     * 目标类型
     *
     * 枚举 {@link com.meession.etm.module.marketing.enums.CampaignTargetTypeEnum}
     */
    private Integer targetType;

    /**
     * 目标标签（JSON数组，当 targetType 为 BY_TAGS 时使用）
     */
    private String targetTags;

    /**
     * 指定用户ID列表（JSON数组，当 targetType 为 SPECIFIC_USERS 时使用）
     */
    private String targetUserIds;

    /**
     * 计划发送时间
     */
    private LocalDateTime sendTime;

    /**
     * 活动结束时间
     */
    private LocalDateTime endTime;

    /**
     * BPM 流程实例ID（群发审核）
     */
    private String bpmProcessInstanceId;

    /**
     * 已发送数量
     */
    private Integer sentCount;

    /**
     * 成功数量
     */
    private Integer successCount;

    /**
     * 失败数量
     */
    private Integer failCount;

    /**
     * 备注
     */
    private String remark;

}
