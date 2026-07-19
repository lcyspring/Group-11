package com.meession.etm.module.erp.api.sale.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.experimental.Accessors;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Accessors(chain = true)
public class ErpSaleOrderCreateReqDTO {

    @NotBlank
    @Size(max = 32)
    private String sourceSystem;
    @NotBlank
    @Size(max = 32)
    private String sourceType;
    @NotNull
    private Long sourceId;
    @NotBlank
    @Size(max = 96)
    private String requestId;
    @NotBlank
    @Size(min = 64, max = 64)
    private String requestHash;
    @NotNull
    private Long customerId;
    @NotNull
    private LocalDateTime orderTime;
    private Long saleUserId;
    private Long accountId;
    @NotNull
    @DecimalMin("0")
    private BigDecimal discountPercent;
    @NotBlank
    @Size(min = 3, max = 3)
    private String currencyCode;
    @NotBlank
    @Size(min = 3, max = 3)
    private String sourceCurrencyCode;
    @NotNull
    @DecimalMin(value = "0", inclusive = false)
    private BigDecimal exchangeRateToOrderCurrency;
    @NotNull
    @DecimalMin("0")
    private BigDecimal sourceGrossAmount;
    @NotNull
    @DecimalMin("0")
    private BigDecimal expectedTotalPrice;
    @Size(max = 1000)
    private String remark;
    @Valid
    @NotEmpty
    private List<Item> items;

    @Data
    @Accessors(chain = true)
    public static class Item {

        @NotNull
        private Long productId;
        @NotNull
        @DecimalMin("0")
        private BigDecimal productPrice;
        @NotNull
        @DecimalMin(value = "0", inclusive = false)
        private BigDecimal count;
        @NotNull
        @DecimalMin("0")
        private BigDecimal taxPercent;
        @Size(max = 500)
        private String remark;
    }
}
