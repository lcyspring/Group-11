package com.meession.etm.module.crm.controller.admin.customer.vo.garbage;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CrmCustomerGarbagePutReqVOTest {

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    void acceptsCustomerAndReason() {
        assertEquals(0, validator.validate(new CrmCustomerGarbagePutReqVO()
                .setCustomerId(1L).setReason("无法联系")).size());
    }

    @Test
    void rejectsBlankAndOversizedReason() {
        assertTrue(validator.validate(new CrmCustomerGarbagePutReqVO()
                .setCustomerId(1L).setReason(" ")).size() > 0);
        assertTrue(validator.validate(new CrmCustomerGarbagePutReqVO()
                .setCustomerId(1L).setReason("x".repeat(501))).size() > 0);
    }
}
