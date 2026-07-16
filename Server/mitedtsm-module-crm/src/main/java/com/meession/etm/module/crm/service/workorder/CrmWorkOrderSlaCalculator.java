package com.meession.etm.module.crm.service.workorder;

import com.meession.etm.module.crm.dal.dataobject.workorder.CrmWorkOrderHolidayDO;
import com.meession.etm.module.crm.dal.dataobject.workorder.CrmWorkOrderSlaPolicyDO;
import com.meession.etm.module.crm.dal.mysql.workorder.CrmWorkOrderHolidayMapper;
import com.meession.etm.module.crm.framework.workorder.CrmWorkOrderGovernanceProperties;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Component;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;

/** Calculates SLA deadlines in working minutes and skips configured holidays/weekends. */
@Component
public class CrmWorkOrderSlaCalculator {
    @Resource
    private CrmWorkOrderHolidayMapper holidayMapper;
    @Resource
    private CrmWorkOrderGovernanceProperties properties;

    public LocalDateTime plusWorkingMinutes(LocalDateTime start, int minutes) {
        if (minutes <= 0) return start;
        ZoneId zone = ZoneId.of(properties.getSla().getZone());
        LocalDateTime cursor = start.atZone(zone).toLocalDateTime();
        int remaining = minutes;
        while (remaining > 0) {
            if (!isWorkingDay(cursor.toLocalDate())) {
                cursor = LocalDateTime.of(cursor.toLocalDate().plusDays(1), properties.getSla().getWorkdayStart());
                continue;
            }
            LocalTime startTime = properties.getSla().getWorkdayStart();
            LocalTime endTime = properties.getSla().getWorkdayEnd();
            if (cursor.toLocalTime().isBefore(startTime)) cursor = LocalDateTime.of(cursor.toLocalDate(), startTime);
            if (!cursor.toLocalTime().isBefore(endTime)) {
                cursor = LocalDateTime.of(cursor.toLocalDate().plusDays(1), startTime);
                continue;
            }
            long available = java.time.Duration.between(cursor, LocalDateTime.of(cursor.toLocalDate(), endTime)).toMinutes();
            if (remaining <= available) return cursor.plusMinutes(remaining);
            remaining -= (int) available;
            cursor = LocalDateTime.of(cursor.toLocalDate().plusDays(1), startTime);
        }
        return cursor;
    }

    public LocalDateTime responseDue(LocalDateTime start, CrmWorkOrderSlaPolicyDO policy) {
        return plusWorkingMinutes(start, policy.getResponseMinutes());
    }

    public LocalDateTime escalationDue(LocalDateTime start, CrmWorkOrderSlaPolicyDO policy) {
        int minutes = Math.max(0, policy.getResolutionMinutes() - policy.getEscalationMinutes());
        return plusWorkingMinutes(start, minutes);
    }

    public LocalDateTime resolutionDue(LocalDateTime start, CrmWorkOrderSlaPolicyDO policy) {
        return plusWorkingMinutes(start, policy.getResolutionMinutes());
    }

    public boolean isWorkingDay(LocalDate date) {
        CrmWorkOrderHolidayDO override = holidayMapper.selectByDate(date);
        if (override != null) return Boolean.TRUE.equals(override.getWorkingDay());
        DayOfWeek day = date.getDayOfWeek();
        return !properties.getSla().isSkipWeekends()
                || (day != DayOfWeek.SATURDAY && day != DayOfWeek.SUNDAY);
    }
}
