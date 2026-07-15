package com.meession.etm.module.crm.controller.admin.quote.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class CrmQuoteRespVO {
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
    private LocalDateTime createTime;
    private List<Item> items;
    private List<Action> actions;

    @Data
    public static class Item {
        private Long id;
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

    @Data
    public static class Action {
        private Long id;
        private Integer actionType;
        private Integer fromStatus;
        private Integer toStatus;
        private Long operatorUserId;
        private String remark;
        private LocalDateTime createTime;
    }
}
