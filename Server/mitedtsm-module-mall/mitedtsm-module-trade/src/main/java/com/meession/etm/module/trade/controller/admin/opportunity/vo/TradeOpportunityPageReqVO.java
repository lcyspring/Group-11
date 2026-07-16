package com.meession.etm.module.trade.controller.admin.opportunity.vo;

import com.meession.etm.framework.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

@Schema(description = "管理后台 - 商机分页 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
public class TradeOpportunityPageReqVO extends PageParam {

    @Schema(description = "商机名称", example = "采购")
    private String name;

    @Schema(description = "客户名称", example = "XX公司")
    private String customerName;

    @Schema(description = "状态", example = "0")
    private Integer status;

    @Schema(description = "销售用户编号", example = "1")
    private Long salesUserId;

    @Schema(description = "创建时间")
    private LocalDateTime[] createTime;

}