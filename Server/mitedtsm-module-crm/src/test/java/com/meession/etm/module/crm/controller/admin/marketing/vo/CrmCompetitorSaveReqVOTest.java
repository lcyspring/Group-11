package com.meession.etm.module.crm.controller.admin.marketing.vo;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CrmCompetitorSaveReqVOTest {

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    void validCompetitorPasses() {
        assertEquals(0, validator.validate(validRequest()).size());
    }

    @Test
    void identityAndStatusAreRequired() {
        CrmCompetitorSaveReqVO request = validRequest();
        request.setName(" ");
        request.setOwnerUserId(null);
        request.setStatus(null);
        assertEquals(3, validator.validate(request).size());
    }

    @Test
    void websiteMustUseHttpOrHttps() {
        CrmCompetitorSaveReqVO request = validRequest();
        request.setWebsite("javascript:alert(1)");
        assertEquals(1, validator.validate(request).size());
    }

    @Test
    void statusMustUseCommonStatusEnum() {
        CrmCompetitorSaveReqVO request = validRequest();
        request.setStatus(9);
        assertEquals(1, validator.validate(request).size());
    }

    @Test
    void analysisFieldsHaveExplicitLimits() {
        CrmCompetitorSaveReqVO request = validRequest();
        request.setStrengths("x".repeat(2001));
        request.setWeaknesses("x".repeat(2001));
        request.setStrategy("x".repeat(2001));
        request.setRemark("x".repeat(1001));
        assertEquals(4, validator.validate(request).size());
    }

    private static CrmCompetitorSaveReqVO validRequest() {
        CrmCompetitorSaveReqVO request = new CrmCompetitorSaveReqVO();
        request.setName("竞争对手");
        request.setWebsite("https://competitor.example.com");
        request.setOwnerUserId(1L);
        request.setStatus(0);
        return request;
    }
}
