package com.meession.etm.module.crm.framework.marketing;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CrmMarketingPropertiesTest {
    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    void acceptsRecordOnlyPolicy() {
        CrmMarketingProperties properties = new CrmMarketingProperties();
        assertTrue(validator.validate(properties).isEmpty());
    }

    @Test
    void rejectsOversizedBatchAndUnknownProvider() {
        CrmMarketingProperties properties = new CrmMarketingProperties();
        properties.setProviderMode("unknown").setBatchSize(501).setMaxBatchSize(500);
        assertFalse(validator.validate(properties).isEmpty());
    }
}
