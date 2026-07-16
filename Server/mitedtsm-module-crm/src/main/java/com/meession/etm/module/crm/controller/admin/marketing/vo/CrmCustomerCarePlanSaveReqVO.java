package com.meession.etm.module.crm.controller.admin.marketing.vo;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CrmCustomerCarePlanSaveReqVO {
    private Long id; @NotBlank private String code; @NotBlank private String name;
    @NotNull private Integer ruleType; @NotBlank private String eventMonthDay;
    @NotNull private Integer channel; private String smsTemplateCode; private String mailTemplateCode;
    @NotNull private Boolean enabled; private String targetScope;
}
