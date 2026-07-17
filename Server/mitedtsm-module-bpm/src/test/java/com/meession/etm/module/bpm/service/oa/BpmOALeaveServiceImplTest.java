package com.meession.etm.module.bpm.service.oa;

import com.meession.etm.module.bpm.controller.admin.oa.vo.BpmOALeaveCreateReqVO;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class BpmOALeaveServiceImplTest {

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    void calculateWorkingDaysIncludesWeekdaysAndExcludesWeekend() {
        assertEquals(1, BpmOALeaveServiceImpl.calculateWorkingDays(
                LocalDate.of(2026, 7, 17), LocalDate.of(2026, 7, 17)));
        assertEquals(2, BpmOALeaveServiceImpl.calculateWorkingDays(
                LocalDate.of(2026, 7, 17), LocalDate.of(2026, 7, 20)));
    }

    @Test
    void requestRequiresStrictTimeOrderAndMeaningfulReason() {
        LocalDateTime time = LocalDateTime.of(2026, 7, 17, 9, 0);
        BpmOALeaveCreateReqVO request = new BpmOALeaveCreateReqVO()
                .setType(1).setReason("太短").setStartTime(time).setEndTime(time);

        assertFalse(validator.validate(request).isEmpty());
    }
}
