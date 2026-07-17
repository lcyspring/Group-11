package com.meession.etm.module.crm.controller.admin.visit.vo;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class CrmCustomerVisitResultReqVO {
    @NotNull private Long id;
    @NotNull(message = "实际开始时间不能为空") private LocalDateTime actualStartTime;
    @NotNull(message = "实际结束时间不能为空") private LocalDateTime actualEndTime;
    @NotBlank(message = "拜访结果不能为空") @Size(min = 5, max = 2000, message = "拜访结果需填写 5 至 2000 个字符")
    private String resultContent;
    @NotNull(message = "下次联系时间不能为空") private LocalDateTime nextContactTime;
    @Size(max = 10, message = "结果附件不能超过 10 个") private List<String> resultAttachmentUrls;

    @AssertTrue(message = "实际结束时间必须晚于或等于实际开始时间")
    public boolean isActualTimeValid() {
        return actualStartTime == null || actualEndTime == null || !actualEndTime.isBefore(actualStartTime);
    }
}
