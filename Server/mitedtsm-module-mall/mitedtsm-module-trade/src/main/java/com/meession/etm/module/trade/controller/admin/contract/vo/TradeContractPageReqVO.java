package com.meession.etm.module.trade.controller.admin.contract.vo;

import com.meession.etm.framework.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

@Schema(description = "管理后台 - 合同分页 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
public class TradeContractPageReqVO extends PageParam {

    @Schema(description = "合同名称", example = "采购")
    private String name;

    @Schema(description = "合同编号", example = "CT2024")
    private String no;

    @Schema(description = "订单编号", example = "1")
    private Long orderId;

    @Schema(description = "状态", example = "0")
    private Integer status;

    @Schema(description = "客户编号", example = "1")
    private Long customerId;

    @Schema(description = "创建时间")
    private LocalDateTime[] createTime;

}