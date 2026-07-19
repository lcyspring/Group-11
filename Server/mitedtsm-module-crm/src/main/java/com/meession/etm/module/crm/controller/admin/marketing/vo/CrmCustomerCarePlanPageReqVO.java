package com.meession.etm.module.crm.controller.admin.marketing.vo;

import com.meession.etm.framework.common.pojo.PageParam;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data @EqualsAndHashCode(callSuper = true)
public class CrmCustomerCarePlanPageReqVO extends PageParam {
    private String code; private String name; private Integer ruleType; private Integer channel; private Boolean enabled;
}
