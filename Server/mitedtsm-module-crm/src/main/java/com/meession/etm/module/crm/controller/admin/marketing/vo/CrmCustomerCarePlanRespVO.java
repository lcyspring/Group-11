package com.meession.etm.module.crm.controller.admin.marketing.vo;

import lombok.Data;

@Data
public class CrmCustomerCarePlanRespVO {
    private Long id; private String code; private String name; private Integer ruleType;
    private String eventMonthDay; private Integer channel; private String smsTemplateCode;
    private String mailTemplateCode; private Boolean enabled; private String targetScope;
}
