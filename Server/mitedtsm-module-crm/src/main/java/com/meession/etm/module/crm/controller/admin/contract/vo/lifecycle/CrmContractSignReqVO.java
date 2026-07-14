package com.meession.etm.module.crm.controller.admin.contract.vo.lifecycle;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class CrmContractSignReqVO {

    @NotNull
    private Long contractId;
    @NotNull
    @Min(1)
    @Max(2)
    private Integer method;
    @NotNull
    @PastOrPresent
    private LocalDateTime signedTime;
    @NotNull
    private Long signedAttachmentId;
    @NotNull
    private Long handlerUserId;
}
