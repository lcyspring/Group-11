package com.meession.etm.module.trade.dal.dataobject.opportunity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.meession.etm.framework.mybatis.core.dataobject.BaseDO;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

@TableName("trade_opportunity_item")
@Data
@EqualsAndHashCode(callSuper = true)
public class TradeOpportunityItemDO extends BaseDO {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long opportunityId;

    private Long spuId;

    private Long skuId;

    private String productName;

    private String skuName;

    private BigDecimal price;

    private Integer count;

    private BigDecimal totalPrice;

}