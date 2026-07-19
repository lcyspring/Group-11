package com.meession.etm.module.crm.controller.admin.statistics.vo.workorder;

import com.meession.etm.framework.common.enums.DateIntervalEnum;
import com.meession.etm.framework.common.validation.InEnum;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;

import static com.meession.etm.framework.common.util.date.DateUtils.FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND;

@Schema(description = "管理后台 - CRM 工单统计 Request VO")
@Data
public class CrmStatisticsWorkOrderReqVO {

    @Schema(description = "时间间隔", requiredMode = Schema.RequiredMode.REQUIRED, example = "2")
    @NotNull(message = "时间间隔不能为空")
    @InEnum(value = DateIntervalEnum.class, message = "时间间隔必须是 {value}")
    private Integer interval;

    @Schema(description = "时间范围", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "时间范围不能为空")
    @Size(min = 2, max = 2, message = "请选择时间范围")
    @DateTimeFormat(pattern = FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND)
    private LocalDateTime[] times;
}
