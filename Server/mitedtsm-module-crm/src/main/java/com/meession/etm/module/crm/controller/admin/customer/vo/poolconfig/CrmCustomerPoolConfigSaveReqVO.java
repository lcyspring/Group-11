package com.meession.etm.module.crm.controller.admin.customer.vo.poolconfig;

import cn.hutool.core.util.BooleanUtil;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.mzt.logapi.starter.annotation.DiffLogField;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.Objects;

@Schema(description = "管理后台 - CRM 客户公海配置的创建/更新 Request VO")
@Data
public class CrmCustomerPoolConfigSaveReqVO {

    @Schema(description = "是否启用客户公海", requiredMode = Schema.RequiredMode.REQUIRED, example = "true")
    @DiffLogField(name = "是否启用客户公海")
    @NotNull(message = "是否启用客户公海不能为空")
    private Boolean enabled;

    @Schema(description = "未跟进放入公海天数", example = "2")
    @DiffLogField(name = "未跟进放入公海天数")
    @Min(value = 1, message = "未跟进放入公海天数必须大于 0")
    @Max(value = 3650, message = "未跟进放入公海天数不能超过 3650")
    private Integer contactExpireDays;

    @Schema(description = "未成交放入公海天数", example = "2")
    @DiffLogField(name = "未成交放入公海天数")
    @Min(value = 1, message = "未成交放入公海天数必须大于 0")
    @Max(value = 3650, message = "未成交放入公海天数不能超过 3650")
    private Integer dealExpireDays;

    @Schema(description = "是否开启提前提醒", example = "true")
    @DiffLogField(name = "是否开启提前提醒")
    private Boolean notifyEnabled;

    @Schema(description = "提前提醒天数", example = "2")
    @DiffLogField(name = "提前提醒天数")
    @Min(value = 1, message = "提前提醒天数必须大于 0")
    @Max(value = 3650, message = "提前提醒天数不能超过 3650")
    private Integer notifyDays;

    @Schema(description = "每日自助领取上限", requiredMode = Schema.RequiredMode.REQUIRED, example = "10")
    @NotNull(message = "每日领取上限不能为空")
    @Min(value = 1, message = "每日领取上限必须大于 0")
    @Max(value = 1000, message = "每日领取上限不能超过 1000")
    private Integer dailyClaimLimit;

    @Schema(description = "重复领取冷却天数", requiredMode = Schema.RequiredMode.REQUIRED, example = "30")
    @NotNull(message = "重复领取冷却天数不能为空")
    @Min(value = 0, message = "重复领取冷却天数不能小于 0")
    @Max(value = 3650, message = "重复领取冷却天数不能超过 3650")
    private Integer repeatClaimCooldownDays;

    @Schema(description = "重点客户等级阈值", requiredMode = Schema.RequiredMode.REQUIRED, example = "4")
    @NotNull(message = "重点客户等级阈值不能为空")
    @Min(value = 1, message = "重点客户等级阈值必须大于 0")
    @Max(value = 5, message = "重点客户等级阈值不能超过 5")
    private Integer highValueLevelThreshold;

    @Schema(description = "重点客户保护期倍数", requiredMode = Schema.RequiredMode.REQUIRED, example = "2")
    @NotNull(message = "重点客户保护期倍数不能为空")
    @Min(value = 1, message = "重点客户保护期倍数必须大于 0")
    @Max(value = 10, message = "重点客户保护期倍数不能超过 10")
    private Integer highValueExpireMultiplier;

    @Schema(description = "保护活跃商机", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "保护活跃商机配置不能为空")
    private Boolean protectActiveBusiness;

    @Schema(description = "保护未完结销售单据", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "保护未完结销售单据配置不能为空")
    private Boolean protectActiveContract;

    @Schema(description = "自动回收批量", requiredMode = Schema.RequiredMode.REQUIRED, example = "500")
    @NotNull(message = "自动回收批量不能为空")
    @Min(value = 1, message = "自动回收批量必须大于 0")
    private Integer autoPoolBatchSize;

    @AssertTrue(message = "未成交放入公海天数不能为空")
    @JsonIgnore
    public boolean isDealExpireDaysValid() {
        if (!BooleanUtil.isTrue(getEnabled())) {
            return true;
        }
        return Objects.nonNull(getDealExpireDays());
    }

    @AssertTrue(message = "未跟进放入公海天数不能为空")
    @JsonIgnore
    public boolean isContactExpireDaysValid() {
        if (!BooleanUtil.isTrue(getEnabled())) {
            return true;
        }
        return Objects.nonNull(getContactExpireDays());
    }

    @AssertTrue(message = "提前提醒天数不能为空")
    @JsonIgnore
    public boolean isNotifyDaysValid() {
        if (!BooleanUtil.isTrue(getNotifyEnabled())) {
            return true;
        }
        return Objects.nonNull(getNotifyDays());
    }

}
