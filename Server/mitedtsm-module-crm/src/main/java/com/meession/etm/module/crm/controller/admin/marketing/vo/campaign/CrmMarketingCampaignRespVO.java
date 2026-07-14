package com.meession.etm.module.crm.controller.admin.marketing.vo.campaign;

import cn.idev.excel.annotation.ExcelIgnoreUnannotated;
import cn.idev.excel.annotation.ExcelProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Schema(description = "管理后台 - CRM 营销活动 Response VO")
@Data
@ExcelIgnoreUnannotated
public class CrmMarketingCampaignRespVO {

    @Schema(description = "编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1024")
    @ExcelProperty("编号")
    private Long id;

    @Schema(description = "活动名称", requiredMode = Schema.RequiredMode.REQUIRED, example = "双十一促销活动")
    @ExcelProperty("活动名称")
    private String name;

    @Schema(description = "活动类型", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    @ExcelProperty("活动类型")
    private Integer type;

    @Schema(description = "活动状态", requiredMode = Schema.RequiredMode.REQUIRED, example = "0")
    @ExcelProperty("活动状态")
    private Integer status;

    @Schema(description = "活动开始时间")
    @ExcelProperty("活动开始时间")
    private LocalDateTime startTime;

    @Schema(description = "活动结束时间")
    @ExcelProperty("活动结束时间")
    private LocalDateTime endTime;

    @Schema(description = "目标客户编号列表")
    private String targetCustomerIds;

    @Schema(description = "活动描述", example = "双十一促销活动说明")
    @ExcelProperty("活动描述")
    private String description;

    @Schema(description = "活动预算", example = "10000.00")
    @ExcelProperty("活动预算")
    private BigDecimal budget;

    @Schema(description = "实际花费", example = "5000.00")
    @ExcelProperty("实际花费")
    private BigDecimal actualCost;

    @Schema(description = "关联的模板编号", example = "2048")
    private Long templateId;

    @Schema(description = "创建者")
    @ExcelProperty("创建者")
    private String creator;

    @Schema(description = "创建时间", requiredMode = Schema.RequiredMode.REQUIRED)
    @ExcelProperty("创建时间")
    private LocalDateTime createTime;

    @Schema(description = "更新时间", requiredMode = Schema.RequiredMode.REQUIRED)
    @ExcelProperty("更新时间")
    private LocalDateTime updateTime;

}
