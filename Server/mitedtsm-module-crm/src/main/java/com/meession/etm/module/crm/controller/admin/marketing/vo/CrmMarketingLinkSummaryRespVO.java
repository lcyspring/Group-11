package com.meession.etm.module.crm.controller.admin.marketing.vo;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class CrmMarketingLinkSummaryRespVO {
    private Long linkId;
    private String code;
    private String name;
    private String targetUrl;
    private Integer trackedRecipientCount;
    private Integer uniqueClickCount;
    private Integer totalClickCount;
    private BigDecimal uniqueClickRate;
}
