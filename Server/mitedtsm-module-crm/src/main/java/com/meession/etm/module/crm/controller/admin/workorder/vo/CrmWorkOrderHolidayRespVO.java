package com.meession.etm.module.crm.controller.admin.workorder.vo;

import lombok.Data;

import java.time.LocalDate;

@Data
public class CrmWorkOrderHolidayRespVO {
    private Long id;
    private LocalDate holidayDate;
    private String name;
    private Boolean workingDay;
}
