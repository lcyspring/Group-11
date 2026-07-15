package com.meession.etm.module.crm.controller.admin.customer.vo.poolconfig;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Schema(description = "管理后台 - CRM 客户公海规则 Response VO")
@Data
public class CrmCustomerPoolConfigRespVO {

    @Schema(description = "是否启用客户公海", requiredMode = Schema.RequiredMode.REQUIRED, example = "true")
    @NotNull(message = "是否启用客户公海不能为空")
    private Boolean enabled;

    @Schema(description = "未跟进放入公海天数", example = "2")
    private Integer contactExpireDays;

    @Schema(description = "未成交放入公海天数", example = "2")
    private Integer dealExpireDays;

    @Schema(description = "是否开启提前提醒", example = "true")
    private Boolean notifyEnabled;

    @Schema(description = "提前提醒天数", example = "2")
    private Integer notifyDays;

    @Schema(description = "每日自助领取上限")
    private Integer dailyClaimLimit;
    @Schema(description = "重复领取冷却天数")
    private Integer repeatClaimCooldownDays;
    @Schema(description = "重点客户等级阈值")
    private Integer highValueLevelThreshold;
    @Schema(description = "重点客户保护期倍数")
    private Integer highValueExpireMultiplier;
    @Schema(description = "保护活跃商机")
    private Boolean protectActiveBusiness;
    @Schema(description = "保护未完结销售单据")
    private Boolean protectActiveContract;
    @Schema(description = "自动回收批量")
    private Integer autoPoolBatchSize;
    @Schema(description = "自动回收批量的 YAML 安全上限", requiredMode = Schema.RequiredMode.REQUIRED)
    private Integer autoPoolMaxBatchSize;

}
