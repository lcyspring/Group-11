package com.meession.etm.module.crm.controller.admin.workorder.vo;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CrmWorkOrderGovernanceRequestValidationTest {
    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    void checkInRequiresValidCoordinates() {
        CrmWorkOrderCheckInReqVO valid = new CrmWorkOrderCheckInReqVO().setId(1L)
                .setLatitude(BigDecimal.valueOf(31.2)).setLongitude(BigDecimal.valueOf(121.5));
        assertTrue(validator.validate(valid).isEmpty());
        assertFalse(validator.validate(new CrmWorkOrderCheckInReqVO().setId(1L)
                .setLatitude(BigDecimal.valueOf(91)).setLongitude(BigDecimal.valueOf(181))).isEmpty());
    }

    @Test
    void holidayRequiresDateNameAndWorkingFlag() {
        assertFalse(validator.validate(new CrmWorkOrderHolidaySaveReqVO()).isEmpty());
    }
}
