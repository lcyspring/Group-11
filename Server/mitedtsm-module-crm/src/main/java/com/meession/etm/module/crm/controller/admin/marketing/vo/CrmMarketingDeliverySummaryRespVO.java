package com.meession.etm.module.crm.controller.admin.marketing.vo;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class CrmMarketingDeliverySummaryRespVO {
    private Long broadcastId;
    private Integer smsSentCount;
    private Integer smsDeliveredCount;
    private Integer smsFailedCount;
    private BigDecimal smsDeliveryRate;
    private Integer emailSentCount;
    private Integer emailAcceptedCount;
    private Integer emailFailedCount;
    private Integer emailOpenedCount;
    private BigDecimal emailOpenRate;
    private Integer providerPendingCount;
    private Integer unknownCount;
}
