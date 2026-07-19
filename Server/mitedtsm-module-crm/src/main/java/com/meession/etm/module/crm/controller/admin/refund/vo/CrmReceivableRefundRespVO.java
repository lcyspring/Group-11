package com.meession.etm.module.crm.controller.admin.refund.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Schema(description = "管理后台 - CRM 退款/冲销 Response VO")
@Data
public class CrmReceivableRefundRespVO {
    private Long id;
    private String no;
    private Long receivableId;
    private String receivableNo;
    private BigDecimal receivablePrice;
    private Long customerId;
    private String customerName;
    private Long contractId;
    private String contractNo;
    private String contractName;
    private Long ownerUserId;
    private String ownerUserName;
    private Integer type;
    private LocalDateTime refundTime;
    private BigDecimal amount;
    private String reason;
    private String remark;
    private String processInstanceId;
    private Integer auditStatus;
    private String creator;
    private String creatorName;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
