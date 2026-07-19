package com.meession.etm.module.crm.controller.admin.workorder.vo;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;

@Data
public class CrmWorkOrderHolidaySaveReqVO {
    private Long id;
    @NotNull(message = "日期不能为空")
    private LocalDate holidayDate;
    @NotBlank(message = "名称不能为空")
    private String name;
    @NotNull(message = "工作日标识不能为空")
    private Boolean workingDay;
}
