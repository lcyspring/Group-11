package com.meession.etm.module.crm.dal.dataobject.marketing;

import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.meession.etm.framework.mybatis.core.dataobject.BaseDO;
import lombok.Data;
import lombok.EqualsAndHashCode;

@TableName("crm_customer_care_plan")
@KeySequence("crm_customer_care_plan_seq")
@Data
@EqualsAndHashCode(callSuper = true)
public class CrmCustomerCarePlanDO extends BaseDO {
    @TableId private Long id;
    private String code;
    private String name;
    private Integer ruleType;
    private String eventMonthDay;
    private Integer channel;
    private String smsTemplateCode;
    private String mailTemplateCode;
    private Boolean enabled;
    private String targetScope;
}
