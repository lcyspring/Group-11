package com.meession.etm.module.crm.dal.dataobject.customer;

import com.meession.etm.framework.mybatis.core.dataobject.BaseDO;
import com.meession.etm.module.crm.enums.customer.CrmCustomerPoolRuleExecuteTypeEnum;
import com.meession.etm.module.crm.enums.customer.CrmCustomerPoolRuleTypeEnum;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.*;

@TableName(value = "crm_customer_pool_rule")
@KeySequence("crm_customer_pool_rule_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CrmCustomerPoolRuleDO extends BaseDO {

    @TableId
    private Long id;

    private String name;

    private Integer ruleType;

    private Integer executeType;

    private String cronExpression;

    private Boolean enabled;

    private Integer sort;

    private String remark;

    private String config;

}
