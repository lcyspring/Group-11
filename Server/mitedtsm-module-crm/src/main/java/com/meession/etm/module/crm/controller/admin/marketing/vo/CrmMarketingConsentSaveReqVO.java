package com.meession.etm.module.crm.controller.admin.marketing.vo;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

@Data
public class CrmMarketingConsentSaveReqVO {
    @NotNull @Positive private Long customerId;
    private Long contactId;
    @NotNull private Integer channel;
    @NotNull private Integer status;
    private String source;
}
