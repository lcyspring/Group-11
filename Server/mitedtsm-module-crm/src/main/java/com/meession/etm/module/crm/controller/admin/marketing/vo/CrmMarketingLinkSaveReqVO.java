package com.meession.etm.module.crm.controller.admin.marketing.vo;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CrmMarketingLinkSaveReqVO {
    @NotBlank
    @Pattern(regexp = "^[A-Za-z][A-Za-z0-9_]{0,31}$")
    private String code;
    @NotBlank
    @Size(max = 100)
    private String name;
    @NotBlank
    @Size(max = 2000)
    private String targetUrl;
}
