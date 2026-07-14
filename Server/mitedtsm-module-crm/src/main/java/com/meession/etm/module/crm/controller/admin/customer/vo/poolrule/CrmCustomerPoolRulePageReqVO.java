package com.meession.etm.module.crm.controller.admin.customer.vo.poolrule;

import com.meession.etm.framework.common.pojo.PageParam;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class CrmCustomerPoolRulePageReqVO extends PageParam {

    private String name;

    private Integer ruleType;

    private Boolean enabled;

}
