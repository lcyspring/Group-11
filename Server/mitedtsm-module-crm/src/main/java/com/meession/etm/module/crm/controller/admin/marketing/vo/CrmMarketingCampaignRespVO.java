package com.meession.etm.module.crm.controller.admin.marketing.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class CrmMarketingCampaignRespVO {
    private Long id;
    private String code;
    private String name;
    private Integer status;
    private Long ownerUserId;
    private String ownerUserName;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private BigDecimal budgetAmount;
    private BigDecimal actualCostAmount;
    private Integer targetLeadCount;
    private Integer targetCustomerCount;
    private String description;
    private String summary;
    private LocalDateTime lockedTime;
    private LocalDateTime terminatedTime;
    private LocalDateTime completedTime;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
    private List<CrmMarketingRelationReqVO> relations;
}
