package com.meession.etm.module.erp.api.sale.dto;

import lombok.Data;
import lombok.experimental.Accessors;

import java.math.BigDecimal;

@Data
@Accessors(chain = true)
public class ErpSaleOrderRespDTO {

    private Long id;
    private String no;
    private Integer status;
    private BigDecimal totalCount;
    private BigDecimal totalPrice;
    private BigDecimal outCount;
    private BigDecimal returnCount;
    private String sourceSystem;
    private String sourceType;
    private Long sourceId;
    private String requestId;
    private String requestHash;
    private String currencyCode;
    private String sourceCurrencyCode;
}
