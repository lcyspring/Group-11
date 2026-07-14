package com.meession.etm.module.crm.service.customer.bo;

import lombok.Data;

import java.util.List;

@Data
public class CrmCustomerPoolDistributeRuleConfig {

    private Boolean autoDistribute;

    private Integer maxPerUser;

    private List<Long> targetUserIds;

    private List<Long> targetDeptIds;

    private Integer priorityType;

    private Boolean excludeRecentlyReceived;

    private Integer excludeDays;

}
