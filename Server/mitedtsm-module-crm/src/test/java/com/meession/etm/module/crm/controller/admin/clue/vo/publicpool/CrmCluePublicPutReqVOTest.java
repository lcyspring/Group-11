package com.meession.etm.module.crm.controller.admin.clue.vo.publicpool;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CrmCluePublicPutReqVOTest {

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    void acceptsClueAndReason() {
        assertEquals(0, validator.validate(new CrmCluePublicPutReqVO()
                .setClueId(1L).setReason("暂不跟进")).size());
    }

    @Test
    void rejectsBlankAndOversizedReason() {
        assertTrue(validator.validate(new CrmCluePublicPutReqVO()
                .setClueId(1L).setReason(" ")).size() > 0);
        assertTrue(validator.validate(new CrmCluePublicPutReqVO()
                .setClueId(1L).setReason("x".repeat(501))).size() > 0);
    }
}
