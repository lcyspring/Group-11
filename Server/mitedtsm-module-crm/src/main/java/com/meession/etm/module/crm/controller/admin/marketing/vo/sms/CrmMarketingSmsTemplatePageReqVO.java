package com.meession.etm.module.crm.controller.admin.marketing.vo.sms;

import com.meession.etm.framework.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.time.LocalDateTime;

@Schema(description = "管理后台 - CRM 营销短信模板分页 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class CrmMarketingSmsTemplatePageReqVO extends PageParam {

    @Schema(description = "模板名称", example = "促销")
    private String name;

    @Schema(description = "模板编码", example = "PROMOTION_NOTIFY")
    private String code;

    @Schema(description = "启用状态", example = "0")
    private Integer status;

    @Schema(description = "关联的营销活动编号", example = "1024")
    private Long campaignId;

    @Schema(description = "创建时间")
    private LocalDateTime[] createTime;

}
