package com.meession.etm.module.crm.service.statistics;

import cn.hutool.core.collection.ListUtil;
import com.meession.etm.framework.common.enums.DateIntervalEnum;
import com.meession.etm.framework.test.core.ut.BaseMockitoUnitTest;
import com.meession.etm.module.crm.controller.admin.statistics.vo.forecast.CrmStatisticsForecastReqVO;
import com.meession.etm.module.crm.controller.admin.statistics.vo.forecast.CrmStatisticsForecastRespVO;
import com.meession.etm.module.crm.dal.mysql.statistics.CrmStatisticsForecastMapper;
import com.meession.etm.module.system.api.dept.DeptApi;
import com.meession.etm.module.system.api.dept.dto.DeptRespDTO;
import com.meession.etm.module.system.api.user.AdminUserApi;
import com.meession.etm.module.system.api.user.dto.AdminUserRespDTO;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

class CrmStatisticsForecastServiceImplTest extends BaseMockitoUnitTest {

    @InjectMocks
    private CrmStatisticsForecastServiceImpl forecastService;

    @Mock
    private CrmStatisticsForecastMapper forecastMapper;

    @Mock
    private AdminUserApi adminUserApi;

    @Mock
    private DeptApi deptApi;

    @Test
    void testGetForecast() {
        CrmStatisticsForecastReqVO reqVO = new CrmStatisticsForecastReqVO();
        reqVO.setDeptId(1L);
        reqVO.setInterval(DateIntervalEnum.DAY.getInterval());
        reqVO.setTimes(new LocalDateTime[]{LocalDateTime.of(2025, 1, 1, 0, 0), LocalDateTime.of(2025, 12, 31, 23, 59)});
        reqVO.setForecastPeriod(3);

        when(deptApi.getChildDeptList(1L)).thenReturn(Collections.emptyList());
        when(adminUserApi.getUserListByDeptIds(ListUtil.of(1L))).thenReturn(ListUtil.of(new AdminUserRespDTO().setId(1L)));

        when(forecastMapper.selectTotalWinAmount(any())).thenReturn(new BigDecimal("100000"));
        when(forecastMapper.selectWinBusinessCount(any())).thenReturn(10L);
        when(forecastMapper.selectTotalBusinessAmount(any())).thenReturn(new BigDecimal("200000"));
        when(forecastMapper.selectBusinessCount(any())).thenReturn(20L);

        Map<String, Object> historicalData = new HashMap<>();
        historicalData.put("time", "2025-01");
        historicalData.put("win_amount", new BigDecimal("10000"));
        when(forecastMapper.selectWinBusinessByDate(any())).thenReturn(ListUtil.of(historicalData));

        CrmStatisticsForecastRespVO result = forecastService.getForecast(reqVO);

        assertNotNull(result);
        assertTrue(result.getTotalForecastAmount().compareTo(BigDecimal.ZERO) > 0);
        assertEquals(new BigDecimal("100000"), result.getTotalActualAmount());
        assertNotNull(result.getForecastData());
    }

    @Test
    void testGetForecastWithEmptyUserIds() {
        CrmStatisticsForecastReqVO reqVO = new CrmStatisticsForecastReqVO();
        reqVO.setDeptId(999L);
        reqVO.setInterval(DateIntervalEnum.DAY.getInterval());
        reqVO.setTimes(new LocalDateTime[]{LocalDateTime.of(2025, 1, 1, 0, 0), LocalDateTime.of(2025, 12, 31, 23, 59)});
        reqVO.setForecastPeriod(3);

        when(deptApi.getChildDeptList(999L)).thenReturn(Collections.emptyList());
        when(adminUserApi.getUserListByDeptIds(ListUtil.of(999L))).thenReturn(Collections.emptyList());

        CrmStatisticsForecastRespVO result = forecastService.getForecast(reqVO);

        assertNotNull(result);
        assertEquals(BigDecimal.ZERO, result.getTotalForecastAmount());
        assertEquals(BigDecimal.ZERO, result.getTotalActualAmount());
        assertTrue(result.getForecastData().isEmpty());
    }

    @Test
    void testGetForecastWithZeroWinAmount() {
        CrmStatisticsForecastReqVO reqVO = new CrmStatisticsForecastReqVO();
        reqVO.setDeptId(1L);
        reqVO.setInterval(DateIntervalEnum.DAY.getInterval());
        reqVO.setTimes(new LocalDateTime[]{LocalDateTime.of(2025, 1, 1, 0, 0), LocalDateTime.of(2025, 12, 31, 23, 59)});
        reqVO.setForecastPeriod(3);

        when(deptApi.getChildDeptList(1L)).thenReturn(Collections.emptyList());
        when(adminUserApi.getUserListByDeptIds(ListUtil.of(1L))).thenReturn(ListUtil.of(new AdminUserRespDTO().setId(1L)));

        when(forecastMapper.selectTotalWinAmount(any())).thenReturn(BigDecimal.ZERO);
        when(forecastMapper.selectWinBusinessCount(any())).thenReturn(0L);
        when(forecastMapper.selectTotalBusinessAmount(any())).thenReturn(new BigDecimal("200000"));
        when(forecastMapper.selectBusinessCount(any())).thenReturn(20L);

        when(forecastMapper.selectWinBusinessByDate(any())).thenReturn(Collections.emptyList());

        CrmStatisticsForecastRespVO result = forecastService.getForecast(reqVO);

        assertNotNull(result);
        assertEquals(BigDecimal.ZERO, result.getTotalForecastAmount());
        assertEquals(BigDecimal.ZERO, result.getTotalActualAmount());
    }

}