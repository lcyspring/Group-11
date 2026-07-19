package com.meession.etm.module.bpm.controller.admin.oa.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.List;

import static com.meession.etm.framework.common.util.date.DateUtils.FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND;

@Schema(description = "管理后台 - 请假申请创建 Request VO")
@Data
public class BpmOALeaveCreateReqVO {

    @Schema(description = "请假的开始时间", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "开始时间不能为空")
    @DateTimeFormat(pattern = FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND)
    private LocalDateTime startTime;

    @Schema(description = "请假的结束时间", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "结束时间不能为空")
    @DateTimeFormat(pattern = FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND)
    private LocalDateTime endTime;

    @Schema(description = "请假类型-参见 bpm_oa_type 枚举", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    @NotNull(message = "请假类型不能为空")
    private Integer type;

    @Schema(description = "原因", requiredMode = Schema.RequiredMode.REQUIRED, example = "阅读密讯")
    @NotBlank(message = "请假原因不能为空")
    @Size(min = 10, max = 200, message = "请假原因长度必须在 10 到 200 个字符之间")
    private String reason;

    @Schema(description = "附件 URL 列表")
    @Size(max = 10, message = "请假附件不能超过 10 个")
    private List<@Size(max = 1024, message = "附件地址不能超过 1024 个字符") String> attachmentUrls;

    @Schema(description = "发起人自选审批人 Map", example = "{taskKey1: [1, 2]}")
    private Map<String, List<Long>> startUserSelectAssignees;

    @AssertTrue(message = "结束时间，需要在开始时间之后")
    public boolean isEndTimeValid() {
        return startTime == null || endTime == null || endTime.isAfter(startTime);
    }

    @AssertTrue(message = "单张请假申请不能跨自然年度，请按年度拆分申请")
    public boolean isSameCalendarYear() {
        return startTime == null || endTime == null || startTime.getYear() == endTime.getYear();
    }

}
