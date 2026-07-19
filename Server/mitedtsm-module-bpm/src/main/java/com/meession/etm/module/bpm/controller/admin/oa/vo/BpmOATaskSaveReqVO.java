package com.meession.etm.module.bpm.controller.admin.oa.vo;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;
import java.time.LocalDateTime;

@Data
public class BpmOATaskSaveReqVO {
    private Long id;
    @NotBlank @Size(max = 200) private String title;
    @Size(max = 4000) private String description;
    @NotNull private Long assigneeUserId;
    private String participantUserIds;
    private Integer priority = 1;
    @NotNull @Future private LocalDateTime dueTime;
    private Integer reminderMinutes;
    @Size(max = 50) private String businessType;
    private Long businessId;
}
