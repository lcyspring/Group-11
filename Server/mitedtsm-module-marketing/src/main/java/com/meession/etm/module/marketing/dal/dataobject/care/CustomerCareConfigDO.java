package com.meession.etm.module.marketing.dal.dataobject.care;

import com.meession.etm.framework.mybatis.core.dataobject.BaseDO;
import com.meession.etm.framework.tenant.core.aop.TenantIgnore;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.*;

/**
 * 客户关怀配置 DO
 * <p>
 * 注意：节日场景暂仅支持公历固定日期。农历节日需手动配置公历日期。
 *
 * @author MITEDTSM
 */
@TableName("marketing_customer_care_config")
@KeySequence("marketing_customer_care_config_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TenantIgnore
public class CustomerCareConfigDO extends BaseDO {

    @TableId
    private Long id;

    /**
     * 场景：BIRTHDAY / HOLIDAY
     * 枚举 {@link com.meession.etm.module.marketing.enums.CareSceneEnum}
     */
    private String scene;

    /**
     * 配置名称
     */
    private String name;

    /**
     * 发送渠道：SMS / MAIL
     */
    private String channel;

    /**
     * 模板编码
     */
    private String templateCode;

    /**
     * 模板参数模板（支持变量如 {nickname}）
     */
    private String templateParamsTemplate;

    /**
     * 节日日期列表（JSON数组，仅 HOLIDAY 场景，格式 ["01-01", "10-01"]）
     */
    private String holidayDates;

    /**
     * 启用状态
     * 枚举 {@link com.meession.etm.framework.common.enums.CommonStatusEnum}
     */
    private Integer status;

    /**
     * 备注
     */
    private String remark;

}
