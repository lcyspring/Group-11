package com.meession.etm.module.crm.controller.admin.statistics.vo;

import com.meession.etm.module.crm.controller.admin.statistics.vo.performance.CrmPerformanceTargetSaveReqVO;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CrmPerformanceTargetValidationTest {

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    void requiresExactlyTwelveMonthlyTargets() {
        CrmPerformanceTargetSaveReqVO reqVO = validRequest();
        reqVO.setMonthlyTargets(Collections.nCopies(11, BigDecimal.TEN));

        assertEquals(1, validator.validate(reqVO).size());
    }

    @Test
    void rejectsUnknownScopeAndTargetTypes() {
        CrmPerformanceTargetSaveReqVO reqVO = validRequest();
        reqVO.setScopeType(99);
        reqVO.setTargetType(99);

        assertEquals(2, validator.validate(reqVO).size());
    }

    @Test
    void validatesNestedMonthlyTargetValues() {
        CrmPerformanceTargetSaveReqVO reqVO = validRequest();
        reqVO.setMonthlyTargets(Collections.nCopies(12, new BigDecimal("-1")));

        assertEquals(12, validator.validate(reqVO).size());
    }

    private static CrmPerformanceTargetSaveReqVO validRequest() {
        CrmPerformanceTargetSaveReqVO reqVO = new CrmPerformanceTargetSaveReqVO();
        reqVO.setScopeType(1);
        reqVO.setScopeId(0L);
        reqVO.setTargetYear(2026);
        reqVO.setTargetType(1);
        reqVO.setMonthlyTargets(Collections.nCopies(12, BigDecimal.TEN));
        return reqVO;
    }

}
