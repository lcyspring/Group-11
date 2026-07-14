package com.meession.etm.module.crm.controller.admin.marketing.vo.campaign;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static com.meession.etm.framework.common.util.date.DateUtils.FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND;

@Schema(description = "管理后台 - CRM 营销活动创建/更新 Request VO")
@Data
public class CrmMarketingCampaignSaveReqVO {

    @Schema(description = "主键", example = "1024")
    private Long id;

    @Schema(description = "活动名称", requiredMode = Schema.RequiredMode.REQUIRED, example = "双十一促销活动")
    @NotNull(message = "活动名称不能为空")
    private String name;

    @Schema(description = "活动类型", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    @NotNull(message = "活动类型不能为空")
    private Integer type;

    @Schema(description = "活动状态", requiredMode = Schema.RequiredMode.REQUIRED, example = "0")
    @NotNull(message = "活动状态不能为空")
    private Integer status;

    @Schema(description = "活动开始时间")
    @DateTimeFormat(pattern = FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND)
    private LocalDateTime startTime;

    @Schema(description = "活动结束时间")
    @DateTimeFormat(pattern = FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND)
    private LocalDateTime endTime;

    @Schema(description = "目标客户编号列表")
    private String targetCustomerIds;

    @Schema(description = "活动描述", example = "双十一促销活动说明")
    private String description;

    @Schema(description = "活动预算，单位：元", example = "10000.00")
    private BigDecimal budget;

    @Schema(description = "实际花费，单位：元", example = "5000.00")
    private BigDecimal actualCost;

    @Schema(description = "关联的模板编号", example = "2048")
    private Long templateId;

}
