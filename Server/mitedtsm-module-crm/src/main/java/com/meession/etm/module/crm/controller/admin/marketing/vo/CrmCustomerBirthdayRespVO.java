package com.meession.etm.module.crm.controller.admin.marketing.vo;

import lombok.Data;

import java.time.LocalDate;

@Data
public class CrmCustomerBirthdayRespVO {
    private Long customerId;
    private String customerName;
    private Long contactId;
    private String contactName;
    private LocalDate birthday;
    private LocalDate nextBirthday;
    private Integer daysUntil;
    private String mobile;
    private String email;
}
