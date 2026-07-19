package com.meession.etm.module.crm.controller.admin.marketing.vo;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CrmMarketingCampaignActionReqVO {
    @NotNull(message = "活动编号不能为空")
    private Long id;
    private String summary;
}
