package com.meession.etm.module.crm.service.statistics;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.collection.ListUtil;
import cn.hutool.core.util.ObjUtil;
import com.meession.etm.framework.common.pojo.PageResult;
import com.meession.etm.framework.common.util.date.LocalDateTimeUtils;
import com.meession.etm.module.crm.controller.admin.statistics.vo.funnel.*;
import com.meession.etm.module.crm.dal.dataobject.business.CrmBusinessDO;
import com.meession.etm.module.crm.dal.dataobject.business.CrmBusinessStatusDO;
import com.meession.etm.module.crm.dal.mysql.business.CrmBusinessMapper;
import com.meession.etm.module.crm.dal.mysql.statistics.CrmStatisticsFunnelMapper;
import com.meession.etm.module.crm.enums.business.CrmBusinessEndStatusEnum;
import com.meession.etm.module.crm.service.business.CrmBusinessService;
import com.meession.etm.module.crm.service.business.CrmBusinessStatusService;
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
import java.util.Collections;
import java.util.List;

import static com.meession.etm.framework.common.util.collection.CollectionUtils.convertList;

/**
 * CRM 销售漏斗分析 Service 实现类
 *
 * @author HUIHUI
 */
@Service
public class CrmStatisticsFunnelServiceImpl implements CrmStatisticsFunnelService {

    @Resource
    private CrmStatisticsFunnelMapper funnelMapper;
    @Resource
    private CrmBusinessMapper businessMapper;

    @Resource
    private AdminUserApi adminUserApi;
    @Resource
    private CrmBusinessService businessService;
    @Resource
    private CrmBusinessStatusService businessStatusService;
    @Resource
    private DeptApi deptApi;

    @Override
    public CrmStatisticFunnelSummaryRespVO getFunnelSummary(CrmStatisticsFunnelReqVO reqVO) {
        // 1. 获得用户编号数组
        List<Long> userIds = getUserIds(reqVO);
        if (CollUtil.isEmpty(userIds)) {
            return new CrmStatisticFunnelSummaryRespVO(0L, 0L, 0L);
        }
        reqVO.setUserIds(userIds);

        // 2. 获得漏斗数据
        Long customerCount = funnelMapper.selectCustomerCountByDate(reqVO);
        Long businessCount = funnelMapper.selectBusinessCountByDateAndEndStatus(reqVO, null);
        Long businessWinCount = funnelMapper.selectBusinessCountByDateAndEndStatus(reqVO, CrmBusinessEndStatusEnum.WIN.getStatus());
        return new CrmStatisticFunnelSummaryRespVO(customerCount, businessCount, businessWinCount);
    }

    @Override
    public List<CrmStatisticsBusinessStageSummaryRespVO> getBusinessStageSummary(
            CrmStatisticsBusinessStageReqVO reqVO) {
        reqVO.setUserIds(getUserIds(reqVO));
        if (CollUtil.isEmpty(reqVO.getUserIds())) {
            return Collections.emptyList();
        }
        businessStatusService.validateBusinessStatusType(reqVO.getStatusTypeId());
        List<CrmStatisticsBusinessStageSummaryRespVO> rows = funnelMapper.selectBusinessStageSummary(reqVO);
        if (CollUtil.isEmpty(rows)) {
            return Collections.emptyList();
        }

        List<CrmStatisticsBusinessStageSummaryRespVO> stageRows = rows.stream()
                .filter(row -> row.getEndStatus() == null).toList();
        List<CrmStatisticsBusinessStageSummaryRespVO> outcomeRows = rows.stream()
                .filter(row -> row.getEndStatus() != null).toList();

        // SQL 按商机保留的最后阶段返回存量（含活跃和三种终态），由后向前累加得到
        // “至少到达本阶段”的单调漏斗。终态结果独立计算分布，不伪装成线性后续阶段。
        long cumulativeCount = 0L;
        BigDecimal cumulativePrice = BigDecimal.ZERO;
        for (int index = stageRows.size() - 1; index >= 0; index--) {
            CrmStatisticsBusinessStageSummaryRespVO row = stageRows.get(index);
            cumulativeCount += row.getBusinessCount();
            cumulativePrice = cumulativePrice.add(row.getTotalPrice());
            row.setBusinessCount(cumulativeCount);
            row.setTotalPrice(cumulativePrice.setScale(2, RoundingMode.HALF_UP));
        }
        long previousCount = 0L;
        for (int index = 0; index < stageRows.size(); index++) {
            CrmStatisticsBusinessStageSummaryRespVO row = stageRows.get(index);
            row.setConversionRate(index == 0
                    ? (row.getBusinessCount() == 0L ? BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP)
                    : BigDecimal.valueOf(100).setScale(2, RoundingMode.HALF_UP))
                    : calculatePercentage(row.getBusinessCount(), previousCount));
            previousCount = row.getBusinessCount();
        }
        long endedCount = outcomeRows.stream()
                .mapToLong(CrmStatisticsBusinessStageSummaryRespVO::getBusinessCount).sum();
        outcomeRows.forEach(row -> row.setTotalPrice(row.getTotalPrice().setScale(2, RoundingMode.HALF_UP))
                .setConversionRate(calculatePercentage(row.getBusinessCount(), endedCount)));
        List<CrmStatisticsBusinessStageSummaryRespVO> result = new ArrayList<>(stageRows.size() + outcomeRows.size());
        result.addAll(stageRows);
        result.addAll(outcomeRows);
        return result;
    }

    @Override
    public PageResult<CrmBusinessDO> getBusinessStagePage(CrmStatisticsBusinessStagePageReqVO pageVO) {
        pageVO.setUserIds(getUserIds(pageVO));
        if (CollUtil.isEmpty(pageVO.getUserIds())) {
            return PageResult.empty();
        }
        CrmBusinessStatusDO status = businessStatusService.validateBusinessStatus(
                pageVO.getStatusTypeId(), pageVO.getStatusId());
        return businessMapper.selectStagePage(pageVO, status.getSort());
    }

    @Override
    public PageResult<CrmBusinessDO> getBusinessWonPage(CrmStatisticsBusinessStageReqVO pageVO) {
        pageVO.setUserIds(getUserIds(pageVO));
        if (CollUtil.isEmpty(pageVO.getUserIds())) {
            return PageResult.empty();
        }
        businessStatusService.validateBusinessStatusType(pageVO.getStatusTypeId());
        return businessMapper.selectWonPage(pageVO);
    }

    @Override
    public PageResult<CrmBusinessDO> getBusinessOutcomePage(CrmStatisticsBusinessOutcomePageReqVO pageVO) {
        pageVO.setUserIds(getUserIds(pageVO));
        if (CollUtil.isEmpty(pageVO.getUserIds())) {
            return PageResult.empty();
        }
        businessStatusService.validateBusinessStatusType(pageVO.getStatusTypeId());
        return businessMapper.selectOutcomePage(pageVO);
    }

    @Override
    public List<CrmStatisticsBusinessSummaryByEndStatusRespVO> getBusinessSummaryByEndStatus(CrmStatisticsFunnelReqVO reqVO) {
        // 1. 获得用户编号数组
        reqVO.setUserIds(getUserIds(reqVO));
        if (CollUtil.isEmpty(reqVO.getUserIds())) {
            return Collections.emptyList();
        }

        // 2. 获得统计数据
        return funnelMapper.selectBusinessSummaryListGroupByEndStatus(reqVO);
    }

    @Override
    public List<CrmStatisticsBusinessSummaryByDateRespVO> getBusinessSummaryByDate(CrmStatisticsFunnelReqVO reqVO) {
        // 1. 获得用户编号数组
        reqVO.setUserIds(getUserIds(reqVO));
        if (CollUtil.isEmpty(reqVO.getUserIds())) {
            return Collections.emptyList();
        }

        // 2. 按天统计，获取分项统计数据
        List<CrmStatisticsBusinessSummaryByDateRespVO> businessSummaryList = funnelMapper.selectBusinessSummaryGroupByDate(reqVO);
        // 3. 按照日期间隔，合并数据
        List<LocalDateTime[]> timeRanges = LocalDateTimeUtils.getDateRangeList(reqVO.getTimes()[0], reqVO.getTimes()[1], reqVO.getInterval());
        return convertList(timeRanges, times -> {
            Long businessCreateCount = businessSummaryList.stream()
                    .filter(vo -> LocalDateTimeUtils.isBetween(times[0], times[1], vo.getTime()))
                    .mapToLong(CrmStatisticsBusinessSummaryByDateRespVO::getBusinessCreateCount).sum();
            BigDecimal businessDealCount = businessSummaryList.stream()
                    .filter(vo -> LocalDateTimeUtils.isBetween(times[0], times[1], vo.getTime()))
                    .map(CrmStatisticsBusinessSummaryByDateRespVO::getTotalPrice)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            return new CrmStatisticsBusinessSummaryByDateRespVO()
                    .setTime(LocalDateTimeUtils.formatDateRange(times[0], times[1], reqVO.getInterval()))
                    .setBusinessCreateCount(businessCreateCount).setTotalPrice(businessDealCount);
        });
    }

    @Override
    public List<CrmStatisticsBusinessInversionRateSummaryByDateRespVO> getBusinessInversionRateSummaryByDate(CrmStatisticsFunnelReqVO reqVO) {
        // 1. 获得用户编号数组
        reqVO.setUserIds(getUserIds(reqVO));
        if (CollUtil.isEmpty(reqVO.getUserIds())) {
            return Collections.emptyList();
        }

        // 2. 按天统计，获取分项统计数据
        List<CrmStatisticsBusinessInversionRateSummaryByDateRespVO> businessSummaryList = funnelMapper.selectBusinessInversionRateSummaryByDate(reqVO);
        // 3. 按照日期间隔，合并数据
        List<LocalDateTime[]> timeRanges = LocalDateTimeUtils.getDateRangeList(reqVO.getTimes()[0], reqVO.getTimes()[1], reqVO.getInterval());
        return convertList(timeRanges, times -> {
            Long businessCount = businessSummaryList.stream()
                    .filter(vo -> LocalDateTimeUtils.isBetween(times[0], times[1], vo.getTime()))
                    .mapToLong(CrmStatisticsBusinessInversionRateSummaryByDateRespVO::getBusinessCount).sum();
            Long businessWinCount = businessSummaryList.stream()
                    .filter(vo -> LocalDateTimeUtils.isBetween(times[0], times[1], vo.getTime()))
                    .mapToLong(CrmStatisticsBusinessInversionRateSummaryByDateRespVO::getBusinessWinCount).sum();
            return new CrmStatisticsBusinessInversionRateSummaryByDateRespVO()
                    .setTime(LocalDateTimeUtils.formatDateRange(times[0], times[1], reqVO.getInterval()))
                    .setBusinessCount(businessCount).setBusinessWinCount(businessWinCount)
                    .setBusinessWinRate(calculatePercentage(businessWinCount, businessCount));
        });
    }

    @Override
    public List<CrmStatisticsBusinessForecastByDateRespVO> getBusinessForecastByDate(CrmStatisticsFunnelReqVO reqVO) {
        reqVO.setUserIds(getUserIds(reqVO));
        if (CollUtil.isEmpty(reqVO.getUserIds())) {
            return Collections.emptyList();
        }
        List<CrmStatisticsBusinessForecastByDateRespVO> forecastList =
                funnelMapper.selectBusinessForecastGroupByDate(reqVO);
        List<LocalDateTime[]> timeRanges = LocalDateTimeUtils.getDateRangeList(
                reqVO.getTimes()[0], reqVO.getTimes()[1], reqVO.getInterval());
        return convertList(timeRanges, times -> {
            List<CrmStatisticsBusinessForecastByDateRespVO> rows = forecastList.stream()
                    .filter(vo -> LocalDateTimeUtils.isBetween(times[0], times[1], vo.getTime()))
                    .toList();
            long forecastBusinessCount = rows.stream()
                    .mapToLong(CrmStatisticsBusinessForecastByDateRespVO::getForecastBusinessCount).sum();
            long actualBusinessCount = rows.stream()
                    .mapToLong(CrmStatisticsBusinessForecastByDateRespVO::getActualBusinessCount).sum();
            BigDecimal forecastAmount = rows.stream()
                    .map(CrmStatisticsBusinessForecastByDateRespVO::getForecastAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add).setScale(2, RoundingMode.HALF_UP);
            BigDecimal actualAmount = rows.stream()
                    .map(CrmStatisticsBusinessForecastByDateRespVO::getActualAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add).setScale(2, RoundingMode.HALF_UP);
            return new CrmStatisticsBusinessForecastByDateRespVO()
                    .setTime(LocalDateTimeUtils.formatDateRange(times[0], times[1], reqVO.getInterval()))
                    .setForecastBusinessCount(forecastBusinessCount)
                    .setActualBusinessCount(actualBusinessCount)
                    .setForecastAmount(forecastAmount)
                    .setActualAmount(actualAmount);
        });
    }

    /**
     * 计算百分比指标，统一由后端定义零分母及舍入口径。
     */
    private static BigDecimal calculatePercentage(long value, long total) {
        if (total == 0L) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }
        return BigDecimal.valueOf(value)
                .multiply(BigDecimal.valueOf(100))
                .divide(BigDecimal.valueOf(total), 2, RoundingMode.HALF_UP);
    }

    @Override
    public PageResult<CrmBusinessDO> getBusinessPageByDate(CrmStatisticsFunnelReqVO pageVO) {
        // 1. 获得用户编号数组
        pageVO.setUserIds(getUserIds(pageVO));
        if (CollUtil.isEmpty(pageVO.getUserIds())) {
            return PageResult.empty();
        }
        // 2. 执行查询
        return businessService.getBusinessPageByDate(pageVO);
    }

    @Override
    public PageResult<CrmBusinessDO> getBusinessForecastPage(CrmStatisticsFunnelReqVO pageVO) {
        pageVO.setUserIds(getUserIds(pageVO));
        if (CollUtil.isEmpty(pageVO.getUserIds())) {
            return PageResult.empty();
        }
        return businessService.getBusinessForecastPage(pageVO);
    }

    /**
     * 获取用户编号数组。如果用户编号为空, 则获得部门下的用户编号数组，包括子部门的所有用户编号
     *
     * @param reqVO 请求参数
     * @return 用户编号数组
     */
    private List<Long> getUserIds(CrmStatisticsFunnelReqVO reqVO) {
        // 情况一：选中某个用户
        if (ObjUtil.isNotNull(reqVO.getUserId())) {
            return ListUtil.of(reqVO.getUserId());
        }
        // 情况二：选中某个部门
        // 2.1 获得部门列表
        List<Long> deptIds = convertList(deptApi.getChildDeptList(reqVO.getDeptId()), DeptRespDTO::getId);
        deptIds.add(reqVO.getDeptId());
        // 2.2 获得用户编号
        return convertList(adminUserApi.getUserListByDeptIds(deptIds), AdminUserRespDTO::getId);
    }

}
