package com.meession.etm.module.crm.controller.admin.contract.vo.lifecycle;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CrmContractAttachmentCreateReqVO {

    @NotNull
    private Long contractId;
    @NotNull
    @Min(1)
    @Max(3)
    private Integer category;
    @NotBlank
    @Size(max = 255)
    private String fileName;
    @NotBlank
    @Size(max = 1024)
    private String fileUrl;
    @Size(max = 128)
    private String contentType;
    @PositiveOrZero
    private Long fileSize;
    @Pattern(regexp = "[0-9a-fA-F]{64}", message = "SHA-256 必须为 64 位十六进制")
    private String sha256;
}
