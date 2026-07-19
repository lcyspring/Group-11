package com.meession.etm.module.bpm.controller.admin.oa.vo;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.time.LocalDateTime;

@Data
public class BpmOAEventSaveReqVO {
    private Long id;
    @NotBlank private String title;
    private String description;
    @NotNull private LocalDateTime startTime;
    @NotNull private LocalDateTime endTime;
    private Boolean allDay = false;
    private String location;
    private String participantUserIds;
    private Integer reminderMinutes;
    @AssertTrue(message = "日程结束时间必须晚于开始时间")
    public boolean isTimeRangeValid() { return startTime != null && endTime != null && endTime.isAfter(startTime); }
}
