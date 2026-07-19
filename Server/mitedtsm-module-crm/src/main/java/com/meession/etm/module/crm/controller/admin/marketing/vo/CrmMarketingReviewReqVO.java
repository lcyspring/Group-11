package com.meession.etm.module.crm.controller.admin.marketing.vo;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CrmMarketingReviewReqVO {
    @NotNull private Long id;
    private String comment;
}
