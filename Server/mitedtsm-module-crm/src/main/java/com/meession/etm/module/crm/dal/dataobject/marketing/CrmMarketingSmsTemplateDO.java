package com.meession.etm.module.crm.dal.dataobject.marketing;

import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import com.meession.etm.framework.common.enums.CommonStatusEnum;
import com.meession.etm.framework.mybatis.core.dataobject.BaseDO;
import lombok.*;

import java.util.List;

/**
 * CRM 营销短信模板 DO
 *
 * @author mitedtsm
 */
@TableName(value = "crm_marketing_sms_template", autoResultMap = true)
@KeySequence("crm_marketing_sms_template_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CrmMarketingSmsTemplateDO extends BaseDO {

    /**
     * 编号
     */
    @TableId
    private Long id;

    /**
     * 模板名称
     */
    private String name;

    /**
     * 模板编码，保证唯一
     */
    private String code;

    /**
     * 模板内容
     *
     * 内容的参数，使用 {} 包括
     */
    private String content;

    /**
     * 参数数组（自动根据内容生成）
     */
    @TableField(typeHandler = JacksonTypeHandler.class)
    private List<String> params;

    /**
     * 启用状态
     *
     * 枚举 {@link CommonStatusEnum}
     */
    private Integer status;

    /**
     * 关联的营销活动编号
     */
    private Long campaignId;

    /**
     * 短信渠道编号
     */
    private Long channelId;

    /**
     * 短信渠道编码，冗余字段
     */
    private String channelCode;

    /**
     * 短信 API 的模板编号
     */
    private String apiTemplateId;

    /**
     * 备注
     */
    private String remark;

}
