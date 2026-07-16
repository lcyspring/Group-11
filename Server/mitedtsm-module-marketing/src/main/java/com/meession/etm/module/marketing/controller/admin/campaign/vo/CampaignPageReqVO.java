package com.meession.etm.module.marketing.controller.admin.campaign.vo;

import com.meession.etm.framework.common.pojo.PageParam;
import com.meession.etm.framework.common.validation.InEnum;
import com.meession.etm.module.marketing.enums.CampaignStatusEnum;
import com.meession.etm.module.marketing.enums.CampaignTargetTypeEnum;
import com.meession.etm.module.marketing.enums.CampaignTypeEnum;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;

import static com.meession.etm.framework.common.util.date.DateUtils.FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND;

@Schema(description = "管理后台 - 营销活动分页 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class CampaignPageReqVO extends PageParam {

    @Schema(description = "活动名称", example = "双十一")
    private String name;

    @Schema(description = "活动类型", example = "1")
    @InEnum(CampaignTypeEnum.class)
    private Integer type;

    @Schema(description = "活动状态", example = "0")
    @InEnum(CampaignStatusEnum.class)
    private Integer status;

    @Schema(description = "目标类型", example = "1")
    @InEnum(CampaignTargetTypeEnum.class)
    private Integer targetType;

    @Schema(description = "创建时间", example = "[2024-01-01 00:00:00, 2024-01-31 23:59:59]")
    @DateTimeFormat(pattern = FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND)
    private LocalDateTime[] createTime;

}
