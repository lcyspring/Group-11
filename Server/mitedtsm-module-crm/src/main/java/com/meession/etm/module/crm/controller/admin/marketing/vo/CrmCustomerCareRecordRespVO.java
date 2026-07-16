package com.meession.etm.module.crm.controller.admin.marketing.vo;

import lombok.Data;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
public class CrmCustomerCareRecordRespVO {
    private Long id; private Long planId; private Long customerId; private Long contactId;
    private LocalDate eventDate; private Integer channel; private Integer status; private String failureReason;
    private Long providerLogId; private LocalDateTime sentAt;
}
