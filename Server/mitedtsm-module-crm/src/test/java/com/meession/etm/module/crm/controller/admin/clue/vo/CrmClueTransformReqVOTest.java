package com.meession.etm.module.crm.controller.admin.clue.vo;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CrmClueTransformReqVOTest {

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    void transformRequiresClueAndPrimaryContactFields() {
        assertEquals(3, validator.validate(new CrmClueTransformReqVO()).size());
    }

    @Test
    void transformRejectsInvalidPrimaryContactMobile() {
        CrmClueTransformReqVO reqVO = new CrmClueTransformReqVO().setId(10L)
                .setContactName("首联系人").setContactMobile("12345");

        assertEquals(1, validator.validate(reqVO).size());
    }

    @Test
    void transformAcceptsCompletePrimaryContact() {
        CrmClueTransformReqVO reqVO = new CrmClueTransformReqVO().setId(10L)
                .setContactName("首联系人").setContactMobile("13800138000");

        assertEquals(0, validator.validate(reqVO).size());
    }

}
