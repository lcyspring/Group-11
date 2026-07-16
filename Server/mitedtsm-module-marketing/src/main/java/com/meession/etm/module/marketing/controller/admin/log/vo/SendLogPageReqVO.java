package com.meession.etm.module.marketing.controller.admin.log.vo;

import com.meession.etm.framework.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;

import static com.meession.etm.framework.common.util.date.DateUtils.FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND;

@Schema(description = "管理后台 - 营销发送记录分页 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class SendLogPageReqVO extends PageParam {

    @Schema(description = "营销活动编号", example = "1")
    private Long campaignId;

    @Schema(description = "发送渠道", example = "SMS", allowableValues = {"SMS", "MAIL"})
    private String channel;

    @Schema(description = "创建时间", example = "[2024-01-01 00:00:00, 2024-01-31 23:59:59]")
    @DateTimeFormat(pattern = FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND)
    private LocalDateTime[] createTime;

}
