package com.meession.etm.module.crm.controller.admin.statistics.vo.portrait;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Schema(description = "管理后台 - CRM 客户区域维度分析 VO")
@Data
public class CrmStatisticCustomerAreaRespVO {

    @Schema(description = "区域编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "110000")
    private Integer areaId;
    @Schema(description = "区域名称", requiredMode = Schema.RequiredMode.REQUIRED, example = "北京市")
    private String areaName;

    @Schema(description = "客户个数", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    private Integer customerCount;

    @Schema(description = "成交个数", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    private Integer dealCount;

}
