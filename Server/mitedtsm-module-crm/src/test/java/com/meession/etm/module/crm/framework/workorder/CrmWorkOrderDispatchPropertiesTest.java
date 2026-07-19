package com.meession.etm.module.crm.framework.workorder;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CrmWorkOrderDispatchPropertiesTest {

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    void acceptsExplicitDispatchPolicy() {
        assertTrue(validator.validate(valid()).isEmpty());
    }

    @Test
    void rejectsMissingFallbackAndUnsafeLimits() {
        CrmWorkOrderDispatchProperties properties = valid().setFallbackMode(null)
                .setMaxCcUsers(0).setDescriptionMinLength(19).setSolutionMinLength(5001);
        assertEquals(4, validator.validate(properties).size());
    }

    private static CrmWorkOrderDispatchProperties valid() {
        return new CrmWorkOrderDispatchProperties().setEnabled(true).setAutoAssignOnCreate(true)
                .setFallbackMode(CrmWorkOrderDispatchProperties.FallbackMode.UNASSIGNED_POOL)
                .setMaxCcUsers(20).setDescriptionMinLength(20).setSolutionMinLength(20);
    }
}
