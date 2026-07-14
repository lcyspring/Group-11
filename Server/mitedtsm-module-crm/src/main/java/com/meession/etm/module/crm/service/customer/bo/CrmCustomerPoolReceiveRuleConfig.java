package com.meession.etm.module.crm.service.customer.bo;

import lombok.Data;

@Data
public class CrmCustomerPoolReceiveRuleConfig {

    private Integer dailyLimit;

    private Integer weeklyLimit;

    private Integer monthlyLimit;

    private Integer freezeDays;

    private Boolean requirePermission;

    private Integer maxCustomerLevel;

}
