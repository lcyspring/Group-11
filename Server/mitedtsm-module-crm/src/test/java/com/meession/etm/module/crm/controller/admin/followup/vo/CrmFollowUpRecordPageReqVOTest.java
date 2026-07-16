package com.meession.etm.module.crm.controller.admin.followup.vo;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CrmFollowUpRecordPageReqVOTest {
    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    void requiresPositiveBusinessScope() {
        assertEquals(2, validator.validate(new CrmFollowUpRecordPageReqVO()).size());
        assertEquals(2, validator.validate(new CrmFollowUpRecordPageReqVO().setBizType(0).setBizId(0L)).size());
        assertEquals(0, validator.validate(new CrmFollowUpRecordPageReqVO().setBizType(1).setBizId(12L)).size());
    }
}
