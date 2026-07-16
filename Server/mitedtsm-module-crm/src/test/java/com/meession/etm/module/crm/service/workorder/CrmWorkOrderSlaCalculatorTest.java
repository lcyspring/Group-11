package com.meession.etm.module.crm.service.workorder;

import com.meession.etm.module.crm.dal.mysql.workorder.CrmWorkOrderHolidayMapper;
import com.meession.etm.module.crm.framework.workorder.CrmWorkOrderGovernanceProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.time.LocalTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

class CrmWorkOrderSlaCalculatorTest {
    private CrmWorkOrderSlaCalculator calculator;
    private CrmWorkOrderGovernanceProperties properties;
    private CrmWorkOrderHolidayMapper holidayMapper;

    @BeforeEach
    void setUp() {
        calculator = new CrmWorkOrderSlaCalculator();
        properties = new CrmWorkOrderGovernanceProperties();
        properties.getSla().setZone("Asia/Shanghai").setSkipWeekends(true)
                .setWorkdayStart(LocalTime.of(9, 0)).setWorkdayEnd(LocalTime.of(18, 0));
        holidayMapper = Mockito.mock(CrmWorkOrderHolidayMapper.class);
        ReflectionTestUtils.setField(calculator, "properties", properties);
        ReflectionTestUtils.setField(calculator, "holidayMapper", holidayMapper);
        when(holidayMapper.selectByDate(Mockito.any())).thenReturn(null);
    }

    @Test
    void skipsWeekendAndConsumesOnlyWorkingMinutes() {
        LocalDateTime due = calculator.plusWorkingMinutes(LocalDateTime.of(2026, 7, 17, 17, 0), 120);
        assertEquals(LocalDateTime.of(2026, 7, 20, 10, 0), due);
    }

    @Test
    void skipsConfiguredHoliday() {
        when(holidayMapper.selectByDate(java.time.LocalDate.of(2026, 7, 20)))
                .thenReturn(new com.meession.etm.module.crm.dal.dataobject.workorder.CrmWorkOrderHolidayDO().setWorkingDay(false));
        LocalDateTime due = calculator.plusWorkingMinutes(LocalDateTime.of(2026, 7, 17, 17, 0), 120);
        assertEquals(LocalDateTime.of(2026, 7, 21, 10, 0), due);
    }
}
