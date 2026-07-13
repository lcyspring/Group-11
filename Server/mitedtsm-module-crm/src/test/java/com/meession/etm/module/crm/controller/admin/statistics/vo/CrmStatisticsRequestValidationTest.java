package com.meession.etm.module.crm.controller.admin.statistics.vo;

import com.meession.etm.module.crm.controller.admin.statistics.vo.customer.CrmStatisticsCustomerReqVO;
import com.meession.etm.module.crm.controller.admin.statistics.vo.funnel.CrmStatisticsFunnelReqVO;
import com.meession.etm.module.crm.controller.admin.statistics.vo.performance.CrmStatisticsPerformanceReqVO;
import com.meession.etm.module.crm.controller.admin.statistics.vo.portrait.CrmStatisticsPortraitReqVO;
import com.meession.etm.module.crm.controller.admin.statistics.vo.rank.CrmStatisticsRankReqVO;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CrmStatisticsRequestValidationTest {

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    void customerRequiresIntervalAndTimes() {
        CrmStatisticsCustomerReqVO reqVO = new CrmStatisticsCustomerReqVO().setDeptId(1L);

        assertEquals(2, validator.validate(reqVO).size());
    }

    @Test
    void funnelRequiresIntervalAndTimes() {
        CrmStatisticsFunnelReqVO reqVO = new CrmStatisticsFunnelReqVO().setDeptId(1L);

        assertEquals(2, validator.validate(reqVO).size());
    }

    @Test
    void allTimeRangesRequireExactlyTwoValues() {
        LocalDateTime[] oneTime = {LocalDateTime.of(2024, 1, 1, 0, 0)};

        assertEquals(1, validator.validate(new CrmStatisticsPerformanceReqVO()
                .setDeptId(1L).setTimes(oneTime)).size());
        assertEquals(1, validator.validate(new CrmStatisticsPortraitReqVO()
                .setDeptId(1L).setTimes(oneTime)).size());
        assertEquals(1, validator.validate(new CrmStatisticsRankReqVO()
                .setDeptId(1L).setTimes(oneTime)).size());
    }

}
