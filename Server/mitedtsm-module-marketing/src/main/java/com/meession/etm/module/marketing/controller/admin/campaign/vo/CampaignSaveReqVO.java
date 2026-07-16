package com.meession.etm.module.marketing.controller.admin.campaign.vo;

import com.meession.etm.framework.common.validation.InEnum;
import com.meession.etm.module.marketing.enums.CampaignTargetTypeEnum;
import com.meession.etm.module.marketing.enums.CampaignTypeEnum;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;

import static com.meession.etm.framework.common.util.date.DateUtils.FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND;

@Schema(description = "管理后台 - 营销活动创建/修改 Request VO")
@Data
public class CampaignSaveReqVO {

    @Schema(description = "编号", example = "1024")
    private Long id;

    @Schema(description = "活动名称", requiredMode = Schema.RequiredMode.REQUIRED, example = "双十一促销活动")
    @NotEmpty(message = "活动名称不能为空")
    @Size(max = 64, message = "活动名称长度不能超过 64 个字符")
    private String name;

    @Schema(description = "活动类型", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    @NotNull(message = "活动类型不能为空")
    @InEnum(CampaignTypeEnum.class)
    private Integer type;

    @Schema(description = "关联模板ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    @NotNull(message = "关联模板ID不能为空")
    private Long templateId;

    @Schema(description = "目标类型", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    @NotNull(message = "目标类型不能为空")
    @InEnum(CampaignTargetTypeEnum.class)
    private Integer targetType;

    @Schema(description = "目标标签（JSON数组）", example = "[\"VIP\",\"新用户\"]")
    private String targetTags;

    @Schema(description = "指定用户ID列表（JSON数组）", example = "[1,2,3]")
    private String targetUserIds;

    @Schema(description = "计划发送时间")
    @DateTimeFormat(pattern = FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND)
    private LocalDateTime sendTime;

    @Schema(description = "活动结束时间")
    @DateTimeFormat(pattern = FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND)
    private LocalDateTime endTime;

    @Schema(description = "备注", example = "这是双十一的促销短信")
    @Size(max = 256, message = "备注长度不能超过 256 个字符")
    private String remark;

}
