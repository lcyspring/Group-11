package com.meession.etm.module.crm.controller.admin.fulfillment.vo;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CrmErpMappingSaveReqVO {

    @NotNull
    private Long crmId;
    @NotNull
    private Long erpId;
    @Size(max = 500)
    private String remark;
}
