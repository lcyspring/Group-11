package com.meession.etm.module.crm.controller.admin.receivable.vo;

import com.meession.etm.module.crm.controller.admin.receivable.vo.plan.CrmReceivablePlanSaveReqVO;
import com.meession.etm.module.crm.controller.admin.receivable.vo.receivable.CrmReceivableSaveReqVO;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CrmReceivableAmountValidationTest {

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    void receivableRejectsZeroAndNegativeAmounts() {
        CrmReceivableSaveReqVO request = receivableRequest(BigDecimal.ZERO);
        assertEquals("回款金额必须大于 0", validator.validate(request).iterator().next().getMessage());

        request.setPrice(new BigDecimal("-0.01"));
        assertEquals("回款金额必须大于 0", validator.validate(request).iterator().next().getMessage());
    }

    @Test
    void receivablePlanRejectsZeroAndNegativeAmounts() {
        CrmReceivablePlanSaveReqVO request = planRequest(BigDecimal.ZERO);
        assertEquals("计划回款金额必须大于 0", validator.validate(request).iterator().next().getMessage());

        request.setPrice(new BigDecimal("-0.01"));
        assertEquals("计划回款金额必须大于 0", validator.validate(request).iterator().next().getMessage());
    }

    @Test
    void receivableAndPlanAcceptMinimumAmount() {
        assertEquals(0, validator.validate(receivableRequest(new BigDecimal("0.01"))).size());
        assertEquals(0, validator.validate(planRequest(new BigDecimal("0.01"))).size());
    }

    private static CrmReceivableSaveReqVO receivableRequest(BigDecimal price) {
        return new CrmReceivableSaveReqVO()
                .setOwnerUserId(1L)
                .setContractId(20L)
                .setPrice(price)
                .setReturnTime(LocalDateTime.of(2026, 7, 14, 0, 0));
    }

    private static CrmReceivablePlanSaveReqVO planRequest(BigDecimal price) {
        return new CrmReceivablePlanSaveReqVO()
                .setOwnerUserId(1L)
                .setContractId(20L)
                .setPrice(price)
                .setReturnTime(LocalDateTime.of(2026, 7, 14, 0, 0));
    }

}
