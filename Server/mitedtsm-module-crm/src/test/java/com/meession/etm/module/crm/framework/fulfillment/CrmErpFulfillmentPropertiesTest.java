package com.meession.etm.module.crm.framework.fulfillment;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.LinkedHashSet;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CrmErpFulfillmentPropertiesTest {

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    void validatesCompleteExplicitPolicy() {
        CrmErpFulfillmentProperties properties = validProperties();

        assertTrue(validator.validate(properties).isEmpty());
        assertTrue(properties.supportsSourceCurrency(" usd "));
        assertEquals("EUR", CrmErpFulfillmentProperties.normalizeCurrency(" eur "));
    }

    @Test
    void rejectsMissingCurrencyPolicyAndUnsafeLimits() {
        CrmErpFulfillmentProperties properties = validProperties()
                .setCurrencyMode(null)
                .setRoundingMode(null)
                .setTotalTolerance(null)
                .setAmountScale(1)
                .setMaxErrorMessageLength(99);

        assertEquals(5, validator.validate(properties).size());
    }

    private static CrmErpFulfillmentProperties validProperties() {
        return new CrmErpFulfillmentProperties()
                .setEnabled(true)
                .setVersion("2026-07-16")
                .setSourceSystem("CRM")
                .setSourceType("CONTRACT")
                .setErpCurrency("CNY")
                .setAllowedSourceCurrencies(new LinkedHashSet<>(java.util.List.of("CNY", "USD", "EUR")))
                .setCurrencyMode(CrmErpFulfillmentProperties.CurrencyMode.CONVERT_TO_ERP_CURRENCY)
                .setAmountScale(6)
                .setRoundingMode(RoundingMode.HALF_UP)
                .setTotalTolerance(new BigDecimal("0.010000"))
                .setMaxErrorMessageLength(1000);
    }
}
