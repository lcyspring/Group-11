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
 * CRM 营销邮件模板 DO
 *
 * @author mitedtsm
 */
@TableName(value = "crm_marketing_mail_template", autoResultMap = true)
@KeySequence("crm_marketing_mail_template_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CrmMarketingMailTemplateDO extends BaseDO {

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
     * 邮件标题
     *
     * 内容的参数，使用 {} 包括
     */
    private String title;

    /**
     * 邮件内容
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
     * 发送邮箱账号编号
     */
    private Long accountId;

    /**
     * 发送人名称
     */
    private String nickname;

    /**
     * 备注
     */
    private String remark;

}
