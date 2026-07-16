package com.meession.etm.module.crm.controller.admin.marketing.vo;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CrmCustomerCarePlanSaveReqVO {
    private Long id; @NotBlank @Size(max = 64) private String code; @NotBlank @Size(max = 200) private String name;
    @NotNull private Integer ruleType; private String eventMonthDay; private Integer followUpDays;
    @NotNull private Integer channel; private String smsTemplateCode; private String mailTemplateCode;
    @NotNull private Boolean enabled;
}
