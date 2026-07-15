package com.meession.etm.module.crm.dal.dataobject.quote;

import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.meession.etm.framework.mybatis.core.dataobject.BaseDO;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

@TableName("crm_business_quote_item")
@KeySequence("crm_business_quote_item_seq")
@Data
@EqualsAndHashCode(callSuper = true)
public class CrmBusinessQuoteItemDO extends BaseDO {
    @TableId
    private Long id;
    private Long quoteId;
    private Long productId;
    private String productNameSnapshot;
    private String productNoSnapshot;
    private Integer productUnitSnapshot;
    private Long productCategoryIdSnapshot;
    private Integer productVersionSnapshot;
    private BigDecimal listPrice;
    private BigDecimal businessPrice;
    private BigDecimal count;
    private BigDecimal taxRatePercent;
    private BigDecimal lineSubtotal;
    private BigDecimal lineDiscountAmount;
    private BigDecimal netAmount;
    private BigDecimal taxAmount;
    private BigDecimal grossAmount;
}
