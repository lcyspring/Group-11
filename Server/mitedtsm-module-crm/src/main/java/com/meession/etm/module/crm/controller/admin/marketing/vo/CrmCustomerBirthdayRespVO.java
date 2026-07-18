package com.meession.etm.module.crm.controller.admin.marketing.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDate;

@Data
public class CrmCustomerBirthdayRespVO {
    private Integer targetType;
    private Long customerId;
    private String customerName;
    private Long contactId;
    private String contactName;
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate birthday;
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate nextBirthday;
    private Integer daysUntil;
    private String mobile;
    private String email;
}
