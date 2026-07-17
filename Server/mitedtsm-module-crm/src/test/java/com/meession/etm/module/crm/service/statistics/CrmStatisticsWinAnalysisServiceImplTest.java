package com.meession.etm.module.crm.service.statistics;

import cn.hutool.core.collection.ListUtil;
import com.meession.etm.framework.common.enums.DateIntervalEnum;
import com.meession.etm.framework.test.core.ut.BaseMockitoUnitTest;
import com.meession.etm.module.crm.controller.admin.statistics.vo.win.CrmStatisticsWinAnalysisReqVO;
import com.meession.etm.module.crm.controller.admin.statistics.vo.win.CrmStatisticsWinAnalysisRespVO;
import com.meession.etm.module.crm.dal.mysql.statistics.CrmStatisticsWinAnalysisMapper;
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

class CrmStatisticsWinAnalysisServiceImplTest extends BaseMockitoUnitTest {

    @InjectMocks
    private CrmStatisticsWinAnalysisServiceImpl winAnalysisService;

    @Mock
    private CrmStatisticsWinAnalysisMapper winAnalysisMapper;

    @Mock
    private AdminUserApi adminUserApi;

    @Mock
    private DeptApi deptApi;

    @Test
    void testGetWinAnalysis() {
        CrmStatisticsWinAnalysisReqVO reqVO = new CrmStatisticsWinAnalysisReqVO();
        reqVO.setDeptId(1L);
        reqVO.setInterval(DateIntervalEnum.DAY.getInterval());
        reqVO.setTimes(new LocalDateTime[]{LocalDateTime.of(2025, 1, 1, 0, 0), LocalDateTime.of(2025, 12, 31, 23, 59)});

        when(deptApi.getChildDeptList(1L)).thenReturn(Collections.emptyList());
        when(adminUserApi.getUserListByDeptIds(ListUtil.of(1L))).thenReturn(ListUtil.of(new AdminUserRespDTO().setId(1L)));

        Map<String, Object> summary = new HashMap<>();
        summary.put("total_count", 100L);
        summary.put("win_count", 35L);
        summary.put("win_amount", new BigDecimal("1500000"));
        when(winAnalysisMapper.selectBusinessWinSummary(any())).thenReturn(summary);

        CrmStatisticsWinAnalysisRespVO.WinRateByDateItem winRateItem = CrmStatisticsWinAnalysisRespVO.WinRateByDateItem.builder()
                .time("2025-01-01")
                .businessCount(10L)
                .winCount(3L)
                .winRate(new BigDecimal("30.00"))
                .build();
        when(winAnalysisMapper.selectWinRateByDate(any())).thenReturn(ListUtil.of(winRateItem));

        CrmStatisticsWinAnalysisRespVO.WinAmountByIndustryItem industryItem = CrmStatisticsWinAnalysisRespVO.WinAmountByIndustryItem.builder()
                .industry("科技")
                .winAmount(new BigDecimal("500000"))
                .winCount(12L)
                .build();
        when(winAnalysisMapper.selectWinAmountByIndustry(any())).thenReturn(ListUtil.of(industryItem));

        CrmStatisticsWinAnalysisRespVO.WinAmountByCustomerLevelItem levelItem = CrmStatisticsWinAnalysisRespVO.WinAmountByCustomerLevelItem.builder()
                .customerLevel("VIP")
                .winAmount(new BigDecimal("800000"))
                .winCount(20L)
                .build();
        when(winAnalysisMapper.selectWinAmountByCustomerLevel(any())).thenReturn(ListUtil.of(levelItem));

        CrmStatisticsWinAnalysisRespVO result = winAnalysisService.getWinAnalysis(reqVO);

        assertNotNull(result);
        assertEquals(100L, result.getTotalBusinessCount());
        assertEquals(35L, result.getWinBusinessCount());
        assertEquals(new BigDecimal("1500000"), result.getTotalWinAmount());
        assertTrue(result.getWinRate().compareTo(BigDecimal.ZERO) > 0);
        assertNotNull(result.getWinRateByDate());
        assertNotNull(result.getWinAmountByIndustry());
        assertNotNull(result.getWinAmountByCustomerLevel());
    }

    @Test
    void testGetWinAnalysisWithEmptyUserIds() {
        CrmStatisticsWinAnalysisReqVO reqVO = new CrmStatisticsWinAnalysisReqVO();
        reqVO.setDeptId(999L);
        reqVO.setInterval(DateIntervalEnum.DAY.getInterval());
        reqVO.setTimes(new LocalDateTime[]{LocalDateTime.of(2025, 1, 1, 0, 0), LocalDateTime.of(2025, 12, 31, 23, 59)});

        when(deptApi.getChildDeptList(999L)).thenReturn(Collections.emptyList());
        when(adminUserApi.getUserListByDeptIds(ListUtil.of(999L))).thenReturn(Collections.emptyList());

        CrmStatisticsWinAnalysisRespVO result = winAnalysisService.getWinAnalysis(reqVO);

        assertNotNull(result);
        assertEquals(0L, result.getTotalBusinessCount());
        assertEquals(0L, result.getWinBusinessCount());
        assertEquals(BigDecimal.ZERO, result.getTotalWinAmount());
        assertEquals(BigDecimal.ZERO, result.getWinRate());
        assertTrue(result.getWinRateByDate().isEmpty());
        assertTrue(result.getWinAmountByIndustry().isEmpty());
        assertTrue(result.getWinAmountByCustomerLevel().isEmpty());
    }

    @Test
    void testGetWinAnalysisWithZeroBusinessCount() {
        CrmStatisticsWinAnalysisReqVO reqVO = new CrmStatisticsWinAnalysisReqVO();
        reqVO.setDeptId(1L);
        reqVO.setInterval(DateIntervalEnum.DAY.getInterval());
        reqVO.setTimes(new LocalDateTime[]{LocalDateTime.of(2025, 1, 1, 0, 0), LocalDateTime.of(2025, 12, 31, 23, 59)});

        when(deptApi.getChildDeptList(1L)).thenReturn(Collections.emptyList());
        when(adminUserApi.getUserListByDeptIds(ListUtil.of(1L))).thenReturn(ListUtil.of(new AdminUserRespDTO().setId(1L)));

        Map<String, Object> summary = new HashMap<>();
        summary.put("total_count", 0L);
        summary.put("win_count", 0L);
        summary.put("win_amount", BigDecimal.ZERO);
        when(winAnalysisMapper.selectBusinessWinSummary(any())).thenReturn(summary);

        when(winAnalysisMapper.selectWinRateByDate(any())).thenReturn(Collections.emptyList());
        when(winAnalysisMapper.selectWinAmountByIndustry(any())).thenReturn(Collections.emptyList());
        when(winAnalysisMapper.selectWinAmountByCustomerLevel(any())).thenReturn(Collections.emptyList());

        CrmStatisticsWinAnalysisRespVO result = winAnalysisService.getWinAnalysis(reqVO);

        assertNotNull(result);
        assertEquals(0L, result.getTotalBusinessCount());
        assertEquals(0L, result.getWinBusinessCount());
        assertEquals(BigDecimal.ZERO, result.getTotalWinAmount());
        assertEquals(BigDecimal.ZERO, result.getWinRate());
        assertEquals(BigDecimal.ZERO, result.getAvgWinAmount());
    }

}