package com.meession.etm.module.crm.controller.admin.marketing.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class CrmCustomerCarePlanRespVO {
    private Long id; private String code; private String name; private Integer ruleType;
    private String eventMonthDay; private Integer followUpDays; private Integer channel; private String smsTemplateCode;
    private String mailTemplateCode; private Boolean enabled; private String targetScope;
    private LocalDateTime createTime; private LocalDateTime updateTime;
}
