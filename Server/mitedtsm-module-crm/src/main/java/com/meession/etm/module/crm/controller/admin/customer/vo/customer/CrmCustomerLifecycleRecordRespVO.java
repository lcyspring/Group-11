package com.meession.etm.module.crm.controller.admin.customer.vo.customer;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Schema(description = "管理后台 - CRM 客户生命周期变更记录 Response VO")
@Data
public class CrmCustomerLifecycleRecordRespVO {

    private Long id;
    private Long customerId;
    private Integer fromStatus;
    private Integer toStatus;
    private String reason;
    private Long operatorUserId;
    private String operatorUserName;
    private LocalDateTime changeTime;

}
