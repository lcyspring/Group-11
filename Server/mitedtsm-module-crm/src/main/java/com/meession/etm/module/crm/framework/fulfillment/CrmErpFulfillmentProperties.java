package com.meession.etm.module.crm.framework.fulfillment;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Set;

@Component
@Data
@Validated
@ConfigurationProperties(prefix = "mitedtsm.crm.erp-fulfillment")
public class CrmErpFulfillmentProperties {

    private boolean enabled;
    @NotBlank
    private String version;
    @NotBlank
    private String sourceSystem;
    @NotBlank
    private String sourceType;
    @NotBlank
    private String erpCurrency;
    @NotEmpty
    private Set<String> allowedSourceCurrencies = new LinkedHashSet<>();
    @NotNull
    private CurrencyMode currencyMode;
    @Min(2)
    @Max(12)
    private int amountScale;
    @NotNull
    private RoundingMode roundingMode;
    @DecimalMin("0")
    @NotNull
    private BigDecimal totalTolerance;
    @Min(100)
    @Max(1000)
    private int maxErrorMessageLength;
    private Long defaultAccountId;

    public boolean supportsSourceCurrency(String currencyCode) {
        String normalized = normalizeCurrency(currencyCode);
        return allowedSourceCurrencies.stream().map(CrmErpFulfillmentProperties::normalizeCurrency)
                .anyMatch(normalized::equals);
    }

    public static String normalizeCurrency(String currencyCode) {
        return currencyCode == null ? "" : currencyCode.trim().toUpperCase(Locale.ROOT);
    }

    public enum CurrencyMode {
        REQUIRE_SAME_CURRENCY,
        CONVERT_TO_ERP_CURRENCY
    }
}
