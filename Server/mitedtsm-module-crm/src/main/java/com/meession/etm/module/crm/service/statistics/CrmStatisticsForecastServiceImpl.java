package com.meession.etm.module.crm.service.statistics;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.collection.ListUtil;
import cn.hutool.core.util.ObjUtil;
import com.meession.etm.framework.common.util.date.LocalDateTimeUtils;
import com.meession.etm.module.crm.controller.admin.statistics.vo.forecast.CrmStatisticsForecastReqVO;
import com.meession.etm.module.crm.controller.admin.statistics.vo.forecast.CrmStatisticsForecastRespVO;
import com.meession.etm.module.crm.dal.mysql.statistics.CrmStatisticsForecastMapper;
import com.meession.etm.module.system.api.dept.DeptApi;
import com.meession.etm.module.system.api.dept.dto.DeptRespDTO;
import com.meession.etm.module.system.api.user.AdminUserApi;
import com.meession.etm.module.system.api.user.dto.AdminUserRespDTO;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static com.meession.etm.framework.common.util.collection.CollectionUtils.convertList;

@Service
public class CrmStatisticsForecastServiceImpl implements CrmStatisticsForecastService {

    @Resource
    private CrmStatisticsForecastMapper forecastMapper;

    @Resource
    private AdminUserApi adminUserApi;

    @Resource
    private DeptApi deptApi;

    @Override
    public CrmStatisticsForecastRespVO getForecast(CrmStatisticsForecastReqVO reqVO) {
        List<Long> userIds = getUserIds(reqVO);
        if (CollUtil.isEmpty(userIds)) {
            return buildEmptyResult(reqVO);
        }
        reqVO.setUserIds(userIds);

        BigDecimal totalWinAmount = forecastMapper.selectTotalWinAmount(reqVO);
        Long winCount = forecastMapper.selectWinBusinessCount(reqVO);
        BigDecimal totalBusinessAmount = forecastMapper.selectTotalBusinessAmount(reqVO);
        Long businessCount = forecastMapper.selectBusinessCount(reqVO);

        BigDecimal avgWinAmount = winCount > 0 ? totalWinAmount.divide(BigDecimal.valueOf(winCount), 2, RoundingMode.HALF_UP) : BigDecimal.ZERO;
        BigDecimal avgBusinessAmount = businessCount > 0 ? totalBusinessAmount.divide(BigDecimal.valueOf(businessCount), 2, RoundingMode.HALF_UP) : BigDecimal.ZERO;
        BigDecimal winRate = businessCount > 0 ? BigDecimal.valueOf(winCount).divide(BigDecimal.valueOf(businessCount), 4, RoundingMode.HALF_UP) : BigDecimal.ZERO;

        BigDecimal adjustmentFactor = BigDecimal.ONE;
        if (avgBusinessAmount.compareTo(BigDecimal.ZERO) > 0 && avgWinAmount.compareTo(BigDecimal.ZERO) > 0) {
            adjustmentFactor = avgBusinessAmount.multiply(winRate).divide(avgWinAmount, 4, RoundingMode.HALF_UP);
        }

        BigDecimal monthlyForecast = avgWinAmount.multiply(adjustmentFactor);
        BigDecimal totalForecastAmount = monthlyForecast.multiply(BigDecimal.valueOf(reqVO.getForecastPeriod()));

        List<CrmStatisticsForecastRespVO.ForecastItem> forecastData = buildForecastData(reqVO, monthlyForecast);

        return CrmStatisticsForecastRespVO.builder()
                .forecastData(forecastData)
                .totalForecastAmount(totalForecastAmount)
                .totalActualAmount(totalWinAmount)
                .build();
    }

    private List<CrmStatisticsForecastRespVO.ForecastItem> buildForecastData(CrmStatisticsForecastReqVO reqVO, BigDecimal monthlyForecast) {
        List<CrmStatisticsForecastRespVO.ForecastItem> result = new ArrayList<>();

        List<Map<String, Object>> historicalData = forecastMapper.selectWinBusinessByDate(reqVO);
        List<LocalDateTime[]> timeRanges = LocalDateTimeUtils.getDateRangeList(reqVO.getTimes()[0], reqVO.getTimes()[1], reqVO.getInterval());

        for (LocalDateTime[] times : timeRanges) {
            String timeLabel = LocalDateTimeUtils.formatDateRange(times[0], times[1], reqVO.getInterval());
            BigDecimal actualAmount = historicalData.stream()
                    .filter(map -> timeLabel.contains((String) map.get("time")))
                    .map(map -> (BigDecimal) map.get("win_amount"))
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            result.add(CrmStatisticsForecastRespVO.ForecastItem.builder()
                    .time(timeLabel)
                    .actualAmount(actualAmount)
                    .forecastAmount(BigDecimal.ZERO)
                    .build());
        }

        LocalDateTime lastTime = reqVO.getTimes()[1];
        for (int i = 1; i <= reqVO.getForecastPeriod(); i++) {
            LocalDateTime forecastStart = lastTime.plusMonths(i - 1).withDayOfMonth(1);
            LocalDateTime forecastEnd = lastTime.plusMonths(i).withDayOfMonth(1).minusDays(1);
            String timeLabel = LocalDateTimeUtils.formatDateRange(forecastStart, forecastEnd, 3);

            result.add(CrmStatisticsForecastRespVO.ForecastItem.builder()
                    .time(timeLabel)
                    .actualAmount(BigDecimal.ZERO)
                    .forecastAmount(monthlyForecast)
                    .build());
        }

        return result;
    }

    private CrmStatisticsForecastRespVO buildEmptyResult(CrmStatisticsForecastReqVO reqVO) {
        return CrmStatisticsForecastRespVO.builder()
                .forecastData(new ArrayList<>())
                .totalForecastAmount(BigDecimal.ZERO)
                .totalActualAmount(BigDecimal.ZERO)
                .build();
    }

    private List<Long> getUserIds(CrmStatisticsForecastReqVO reqVO) {
        if (ObjUtil.isNotNull(reqVO.getUserId())) {
            return ListUtil.of(reqVO.getUserId());
        }
        List<Long> deptIds = convertList(deptApi.getChildDeptList(reqVO.getDeptId()), DeptRespDTO::getId);
        deptIds.add(reqVO.getDeptId());
        return convertList(adminUserApi.getUserListByDeptIds(deptIds), AdminUserRespDTO::getId);
    }

}