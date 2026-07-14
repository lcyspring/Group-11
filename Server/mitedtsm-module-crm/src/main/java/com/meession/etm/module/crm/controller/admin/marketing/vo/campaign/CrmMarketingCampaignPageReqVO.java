package com.meession.etm.module.crm.controller.admin.marketing.vo.campaign;

import com.meession.etm.framework.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.time.LocalDateTime;

@Schema(description = "管理后台 - CRM 营销活动分页 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class CrmMarketingCampaignPageReqVO extends PageParam {

    @Schema(description = "活动名称", example = "双十一")
    private String name;

    @Schema(description = "活动类型", example = "1")
    private Integer type;

    @Schema(description = "活动状态", example = "0")
    private Integer status;

    @Schema(description = "创建时间")
    private LocalDateTime[] createTime;

}
