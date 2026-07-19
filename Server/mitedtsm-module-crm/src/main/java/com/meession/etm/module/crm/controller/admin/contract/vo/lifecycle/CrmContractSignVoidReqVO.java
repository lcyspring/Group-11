package com.meession.etm.module.crm.controller.admin.contract.vo.lifecycle;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CrmContractSignVoidReqVO {

    @NotNull
    private Long contractId;
    @NotBlank
    @Size(max = 500)
    private String reason;
}
