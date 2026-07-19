package com.meession.etm.module.crm.service.statistics;

import com.meession.etm.framework.common.enums.DateIntervalEnum;
import com.meession.etm.framework.common.util.date.LocalDateTimeUtils;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CrmStatisticsDateRangeTest {

    @Test
    void hourIntervalDoesNotFallThroughToDailyBuckets() {
        List<LocalDateTime[]> ranges = LocalDateTimeUtils.getDateRangeList(
                LocalDateTime.of(2026, 7, 1, 10, 15),
                LocalDateTime.of(2026, 7, 1, 12, 15),
                DateIntervalEnum.HOUR.getInterval());

        assertAll(
                () -> assertEquals(24, ranges.size()),
                () -> assertEquals(LocalDateTime.of(2026, 7, 1, 0, 0), ranges.get(0)[0]),
                () -> assertTrue(ranges.stream().allMatch(range ->
                        Duration.between(range[0], range[1]).compareTo(Duration.ofHours(1)) < 0))
        );
    }
}
