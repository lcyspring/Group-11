package com.meession.etm.module.crm.framework.quote;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
@Data
@Validated
@ConfigurationProperties(prefix = "mitedtsm.crm.quote")
public class CrmQuoteProperties {
    @NotBlank
    private String version;
    @NotBlank
    private String baseCurrency;
    @NotBlank
    private String defaultCurrency;
    @NotEmpty
    private Map<String, BigDecimal> exchangeRatesToBase = new LinkedHashMap<>();
    @NotEmpty
    private List<BigDecimal> allowedTaxRates;
    private BigDecimal defaultTaxRate;
    @Min(2)
    @Max(8)
    private int amountScale;
    @NotBlank
    private String roundingMode;
    @Min(1)
    @Max(100)
    private int maxVersionsPerBusiness;

    public BigDecimal requireExchangeRate(String currencyCode) {
        BigDecimal rate = exchangeRatesToBase.get(normalize(currencyCode));
        if (rate == null || rate.signum() <= 0) {
            throw new IllegalArgumentException("Unsupported CRM quote currency: " + currencyCode);
        }
        return rate;
    }

    public RoundingMode resolvedRoundingMode() {
        return RoundingMode.valueOf(roundingMode);
    }

    @AssertTrue(message = "CRM quote base/default currency must have a positive YAML exchange rate")
    public boolean isCurrencyConfigurationValid() {
        try {
            return requireExchangeRate(baseCurrency).compareTo(BigDecimal.ONE) == 0
                    && requireExchangeRate(defaultCurrency).signum() > 0;
        } catch (RuntimeException ex) {
            return false;
        }
    }

    @AssertTrue(message = "CRM quote tax rates must be unique, non-negative and contain the default rate")
    public boolean isTaxConfigurationValid() {
        if (allowedTaxRates == null || defaultTaxRate == null || !allowedTaxRates.contains(defaultTaxRate)) {
            return false;
        }
        return allowedTaxRates.stream().allMatch(rate -> rate != null && rate.signum() >= 0
                && rate.compareTo(new BigDecimal("100")) <= 0)
                && allowedTaxRates.stream().map(BigDecimal::stripTrailingZeros).distinct().count()
                == allowedTaxRates.size();
    }

    @AssertTrue(message = "CRM quote rounding mode must be a java.math.RoundingMode")
    public boolean isRoundingModeValid() {
        try {
            resolvedRoundingMode();
            return true;
        } catch (RuntimeException ex) {
            return false;
        }
    }

    public static String normalize(String currencyCode) {
        return currencyCode == null ? null : currencyCode.trim().toUpperCase(java.util.Locale.ROOT);
    }
}
