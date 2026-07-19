package com.meession.etm.module.crm.framework.workorder;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;

import java.time.LocalTime;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CrmWorkOrderGovernancePropertiesTest {
    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    void acceptsExplicitGeofenceAndSlaPolicy() {
        CrmWorkOrderGovernanceProperties properties = new CrmWorkOrderGovernanceProperties();
        properties.getGeofence().setDefaultRadiusMeters(300).setMaxAccuracyMeters(100);
        properties.getSla().setZone("Asia/Shanghai").setWorkdayStart(LocalTime.of(9, 0))
                .setWorkdayEnd(LocalTime.of(18, 0));
        assertTrue(validator.validate(properties).isEmpty());
    }

    @Test
    void rejectsInvalidSlaZoneAndRange() {
        CrmWorkOrderGovernanceProperties properties = new CrmWorkOrderGovernanceProperties();
        properties.getSla().setZone("not-a-zone").setWorkdayStart(LocalTime.NOON)
                .setWorkdayEnd(LocalTime.of(8, 0));
        assertFalse(validator.validate(properties).isEmpty());
    }
}
