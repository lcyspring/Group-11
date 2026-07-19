package com.meession.etm.module.crm.controller.admin.visit.vo;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Data
public class CrmCustomerVisitCreateReqVO {
    @NotNull(message = "客户不能为空") private Long customerId;
    private Long contactId;
    @NotNull(message = "计划开始时间不能为空") @Future(message = "计划开始时间不能早于当前时间")
    private LocalDateTime plannedStartTime;
    @NotNull(message = "计划结束时间不能为空") private LocalDateTime plannedEndTime;
    @NotBlank(message = "拜访地点不能为空") @Size(max = 300, message = "拜访地点不能超过 300 个字符")
    private String location;
    @NotBlank(message = "拜访目的不能为空") @Size(min = 5, max = 1000, message = "拜访目的需填写 5 至 1000 个字符")
    private String purpose;
    @Size(max = 20, message = "参与人员不能超过 20 人") private List<Long> participantUserIds;
    @Size(max = 10, message = "附件不能超过 10 个") private List<String> attachmentUrls;
    private Map<String, List<Long>> startUserSelectAssignees;

    @AssertTrue(message = "计划结束时间必须晚于计划开始时间")
    public boolean isPlannedTimeValid() {
        return plannedStartTime == null || plannedEndTime == null || plannedEndTime.isAfter(plannedStartTime);
    }
}
