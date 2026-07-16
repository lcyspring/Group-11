package com.meession.etm.module.crm.controller.admin.marketing.vo;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
public class CrmMarketingCampaignSaveReqVO {
    private Long id;
    @NotBlank(message = "活动编码不能为空")
    private String code;
    @NotBlank(message = "活动名称不能为空")
    private String name;
    @NotNull(message = "活动负责人不能为空")
    private Long ownerUserId;
    @NotNull(message = "活动开始时间不能为空")
    private LocalDateTime startTime;
    @NotNull(message = "活动结束时间不能为空")
    private LocalDateTime endTime;
    @DecimalMin(value = "0", message = "活动预算不能为负数")
    private BigDecimal budgetAmount;
    @Positive(message = "目标线索数必须为正数")
    private Integer targetLeadCount;
    @Positive(message = "目标客户数必须为正数")
    private Integer targetCustomerCount;
    private String description;
    private String summary;
    private List<CrmMarketingRelationReqVO> relations = new ArrayList<>();
}
