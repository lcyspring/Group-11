package com.meession.etm.module.crm.framework.quote;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class CrmQuotePropertiesTest {
    @Test
    void acceptsExplicitCurrencyTaxAndRoundingPolicy() {
        CrmQuoteProperties properties = validProperties();
        assertTrue(properties.isCurrencyConfigurationValid());
        assertTrue(properties.isTaxConfigurationValid());
        assertTrue(properties.isRoundingModeValid());
    }

    @Test
    void rejectsBaseCurrencyRateOtherThanOne() {
        CrmQuoteProperties properties = validProperties();
        properties.getExchangeRatesToBase().put("CNY", new BigDecimal("1.1"));
        assertFalse(properties.isCurrencyConfigurationValid());
    }

    @Test
    void rejectsDefaultTaxOutsideWhitelistAndInvalidRounding() {
        CrmQuoteProperties properties = validProperties();
        properties.setDefaultTaxRate(new BigDecimal("5"));
        properties.setRoundingMode("BANKERS_MAGIC");
        assertFalse(properties.isTaxConfigurationValid());
        assertFalse(properties.isRoundingModeValid());
    }

    public static CrmQuoteProperties validProperties() {
        CrmQuoteProperties properties = new CrmQuoteProperties();
        properties.setVersion("test");
        properties.setBaseCurrency("CNY");
        properties.setDefaultCurrency("CNY");
        LinkedHashMap<String, BigDecimal> rates = new LinkedHashMap<>();
        rates.put("CNY", BigDecimal.ONE);
        rates.put("USD", new BigDecimal("7.2"));
        properties.setExchangeRatesToBase(rates);
        properties.setAllowedTaxRates(List.of(BigDecimal.ZERO, new BigDecimal("13")));
        properties.setDefaultTaxRate(new BigDecimal("13"));
        properties.setAmountScale(6);
        properties.setRoundingMode("HALF_UP");
        properties.setMaxVersionsPerBusiness(50);
        return properties;
    }
}
