package com.meession.etm.module.trade.dal.dataobject.opportunity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.meession.etm.framework.mybatis.core.dataobject.BaseDO;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@TableName("trade_opportunity")
@Data
@EqualsAndHashCode(callSuper = true)
public class TradeOpportunityDO extends BaseDO {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String no;

    private String name;

    private String description;

    private Long customerId;

    private String customerName;

    private Long productId;

    private String productName;

    private BigDecimal amount;

    private Integer quantity;

    private Integer status;

    private Long salesUserId;

    private String salesUserName;

    private LocalDateTime expectedCloseTime;

    private String remark;

    private Long orderId;

    @TableField(exist = false)
    private TradeOpportunityItemDO item;

}