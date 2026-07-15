package com.meession.etm.module.crm.controller.admin.quote.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Data
public class CrmQuotePolicyRespVO {
    private String version;
    private String baseCurrency;
    private String defaultCurrency;
    private Map<String, BigDecimal> exchangeRatesToBase;
    private List<BigDecimal> allowedTaxRates;
    private BigDecimal defaultTaxRate;
    private Integer amountScale;
    private Integer maxVersionsPerBusiness;
}
