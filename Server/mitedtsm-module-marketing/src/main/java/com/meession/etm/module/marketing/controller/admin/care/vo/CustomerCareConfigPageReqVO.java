package com.meession.etm.module.marketing.controller.admin.care.vo;

import com.meession.etm.framework.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;

import static com.meession.etm.framework.common.util.date.DateUtils.FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND;

@Schema(description = "管理后台 - 客户关怀模板配置分页 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class CustomerCareConfigPageReqVO extends PageParam {

    @Schema(description = "配置名称", example = "生日关怀")
    private String name;

    @Schema(description = "场景", example = "BIRTHDAY", allowableValues = {"BIRTHDAY", "HOLIDAY"})
    private String scene;

    @Schema(description = "发送渠道", example = "SMS", allowableValues = {"SMS", "MAIL"})
    private String channel;

    @Schema(description = "启用状态", example = "0")
    private Integer status;

    @Schema(description = "创建时间", example = "[2024-01-01 00:00:00, 2024-01-31 23:59:59]")
    @DateTimeFormat(pattern = FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND)
    private LocalDateTime[] createTime;

}
