package com.meession.etm.module.bpm.controller.admin.oa.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static com.meession.etm.framework.common.util.date.DateUtils.FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND;

@Schema(description = "管理后台 - OA 出差申请创建 Request VO")
@Data
public class BpmOATripCreateReqVO {

    @NotNull(message = "开始时间不能为空")
    @Future(message = "开始时间不能早于当前时间")
    @DateTimeFormat(pattern = FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND)
    private LocalDateTime startTime;

    @NotNull(message = "结束时间不能为空")
    @DateTimeFormat(pattern = FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND)
    private LocalDateTime endTime;

    @NotBlank(message = "目的地不能为空")
    @Size(max = 200, message = "目的地不能超过 200 个字符")
    private String destination;

    @NotBlank(message = "出差原因不能为空")
    @Size(min = 5, max = 1000, message = "出差原因长度必须在 5 到 1000 个字符之间")
    private String reason;

    @DecimalMin(value = "0.00", message = "预计费用不能小于 0")
    private BigDecimal estimatedExpense;

    @Size(max = 20, message = "同行人员不能超过 20 人")
    private List<Long> companionUserIds;

    @Size(max = 10, message = "附件不能超过 10 个")
    private List<@Size(max = 1024, message = "附件地址不能超过 1024 个字符") String> attachmentUrls;

    private Map<String, List<Long>> startUserSelectAssignees;

    @AssertTrue(message = "结束时间必须晚于开始时间")
    public boolean isEndTimeValid() {
        return startTime == null || endTime == null || endTime.isAfter(startTime);
    }
}
