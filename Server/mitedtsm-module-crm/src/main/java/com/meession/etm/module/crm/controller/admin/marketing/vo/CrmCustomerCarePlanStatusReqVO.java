package com.meession.etm.module.crm.controller.admin.marketing.vo;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CrmCustomerCarePlanStatusReqVO {
    @NotNull
    private Long id;
    @NotNull
    private Boolean enabled;
}
