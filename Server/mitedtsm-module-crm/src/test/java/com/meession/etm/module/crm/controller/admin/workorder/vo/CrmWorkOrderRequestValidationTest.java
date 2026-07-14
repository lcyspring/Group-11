package com.meession.etm.module.crm.controller.admin.workorder.vo;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CrmWorkOrderRequestValidationTest {

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    void assignmentRequiresOrderAndHandler() {
        assertEquals(2, validator.validate(new CrmWorkOrderAssignReqVO()).size());
    }

    @Test
    void assignmentRemarkHasExplicitLengthLimit() {
        CrmWorkOrderAssignReqVO reqVO = new CrmWorkOrderAssignReqVO().setId(1L).setHandlerUserId(2L)
                .setRemark("x".repeat(1001));
        assertEquals(1, validator.validate(reqVO).size());
    }

    @Test
    void sceneTypeOnlyAcceptsCreatedOrHandledViews() {
        assertEquals(1, validator.validate(new CrmWorkOrderPageReqVO().setSceneType(0)).size());
        assertEquals(1, validator.validate(new CrmWorkOrderPageReqVO().setSceneType(3)).size());
        assertEquals(0, validator.validate(new CrmWorkOrderPageReqVO().setSceneType(1)).size());
        assertEquals(0, validator.validate(new CrmWorkOrderPageReqVO().setSceneType(2)).size());
    }
}
