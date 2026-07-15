package com.meession.etm.module.crm.dal.dataobject.quote;

import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.meession.etm.framework.mybatis.core.dataobject.BaseDO;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@TableName("crm_business_quote")
@KeySequence("crm_business_quote_seq")
@Data
@EqualsAndHashCode(callSuper = true)
public class CrmBusinessQuoteDO extends BaseDO {
    @TableId
    private Long id;
    private Long businessId;
    private Integer versionNo;
    private Integer status;
    private Long sourceQuoteId;
    private String currencyCode;
    private String baseCurrencyCode;
    private BigDecimal exchangeRateToBase;
    private BigDecimal discountPercent;
    private BigDecimal subtotal;
    private BigDecimal discountAmount;
    private BigDecimal netAmount;
    private BigDecimal taxAmount;
    private BigDecimal grossAmount;
    private BigDecimal baseGrossAmount;
    private Long lockedBy;
    private LocalDateTime lockedAt;
    private Integer version;
}
