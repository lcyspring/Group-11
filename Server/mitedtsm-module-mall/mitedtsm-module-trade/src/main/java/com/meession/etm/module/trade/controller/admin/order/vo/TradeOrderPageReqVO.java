package com.meession.etm.module.trade.controller.admin.order.vo;

import com.meession.etm.framework.common.pojo.PageParam;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
public class TradeOrderPageReqVO extends PageParam {

    private String no;

    private Integer status;

    private Integer type;

    private Long userId;

    private LocalDateTime[] createTime;

}