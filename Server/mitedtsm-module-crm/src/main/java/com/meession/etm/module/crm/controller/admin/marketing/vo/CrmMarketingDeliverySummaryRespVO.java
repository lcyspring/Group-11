package com.meession.etm.module.crm.controller.admin.marketing.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

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
    private Integer trackedRecipientCount;
    private Integer uniqueClickCount;
    private Integer totalClickCount;
    private BigDecimal uniqueClickRate;
    private List<CrmMarketingLinkSummaryRespVO> links;
}
