package com.meession.etm.module.crm.service.customer.bo;

import lombok.Data;

@Data
public class CrmCustomerPoolRecycleRuleConfig {

    private Integer contactExpireDays;

    private Integer dealExpireDays;

    private Boolean notifyEnabled;

    private Integer notifyDays;

    private Boolean excludeLocked;

    private Boolean excludeDealed;

    private Integer minCustomerLevel;

}
