package com.meession.etm.module.crm.controller.admin.customer.vo.customer;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.springframework.web.multipart.MultipartFile;

@Schema(description = "管理后台 - 客户导入预检 Request VO")
@Data
public class CrmCustomerImportPreviewReqVO {

    @NotNull(message = "Excel 文件不能为空")
    private MultipartFile file;

    @NotNull(message = "是否支持更新不能为空")
    private Boolean updateSupport;

    private Long ownerUserId;

    @Schema(description = "表头到系统字段的 JSON 映射", example = "{\"公司\":\"name\"}")
    private String fieldMapping;
}
