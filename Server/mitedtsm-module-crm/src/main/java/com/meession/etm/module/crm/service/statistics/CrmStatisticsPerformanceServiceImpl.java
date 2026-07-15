package com.meession.etm.module.crm.service.statistics;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.collection.ListUtil;
import cn.hutool.core.util.ObjUtil;
import com.meession.etm.module.crm.controller.admin.statistics.vo.performance.CrmStatisticsPerformanceReqVO;
import com.meession.etm.module.crm.controller.admin.statistics.vo.performance.CrmStatisticsPerformanceRespVO;
import com.meession.etm.module.crm.controller.admin.statistics.vo.performance.CrmStatisticsTargetCompletionReqVO;
import com.meession.etm.module.crm.controller.admin.statistics.vo.performance.CrmStatisticsTargetCompletionRespVO;
import com.meession.etm.module.crm.controller.admin.statistics.vo.performance.CrmStatisticsTargetCompletionSummaryRespVO;
import com.meession.etm.module.crm.dal.dataobject.statistics.CrmPerformanceTargetDO;
import com.meession.etm.module.crm.dal.mysql.statistics.CrmPerformanceTargetMapper;
import com.meession.etm.module.crm.dal.mysql.statistics.CrmStatisticsPerformanceMapper;
import com.meession.etm.module.crm.enums.statistics.CrmPerformanceTargetScopeTypeEnum;
import com.meession.etm.module.crm.enums.statistics.CrmPerformanceTargetTypeEnum;
import com.meession.etm.module.system.api.dept.DeptApi;
import com.meession.etm.module.system.api.dept.dto.DeptRespDTO;
import com.meession.etm.module.system.api.user.AdminUserApi;
import com.meession.etm.module.system.api.user.dto.AdminUserRespDTO;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import static com.meession.etm.framework.common.util.collection.CollectionUtils.convertList;
import static com.meession.etm.framework.common.util.collection.CollectionUtils.convertMap;
import static com.meession.etm.framework.common.exception.util.ServiceExceptionUtil.exception;
import static com.meession.etm.module.crm.enums.ErrorCodeConstants.PERFORMANCE_TARGET_FILTER_SCOPE_MISMATCH;
import static com.meession.etm.module.crm.enums.ErrorCodeConstants.PERFORMANCE_TARGET_PERIOD_INVALID;
import static com.meession.etm.module.crm.enums.ErrorCodeConstants.PERFORMANCE_TARGET_TYPE_INVALID;

/**
 * CRM 员工业绩分析 Service 实现类
 *
 * @author scholar
 */
@Service
@Validated
public class CrmStatisticsPerformanceServiceImpl implements CrmStatisticsPerformanceService {

    @Resource
    private CrmStatisticsPerformanceMapper performanceMapper;
    @Resource
    private CrmPerformanceTargetMapper performanceTargetMapper;

    @Resource
    private AdminUserApi adminUserApi;
    @Resource
    private DeptApi deptApi;

    @Override
    public List<CrmStatisticsPerformanceRespVO> getContractCountPerformance(CrmStatisticsPerformanceReqVO performanceReqVO) {
        return getPerformance(performanceReqVO, performanceMapper::selectContractCountPerformance);
    }

    @Override
    public List<CrmStatisticsPerformanceRespVO> getContractPricePerformance(CrmStatisticsPerformanceReqVO performanceReqVO) {
        return getPerformance(performanceReqVO, performanceMapper::selectContractPricePerformance);
    }

    @Override
    public List<CrmStatisticsPerformanceRespVO> getReceivablePricePerformance(CrmStatisticsPerformanceReqVO performanceReqVO) {
        return getPerformance(performanceReqVO, performanceMapper::selectReceivablePricePerformance);
    }

    @Override
    public CrmStatisticsTargetCompletionSummaryRespVO getTargetCompletion(
            CrmStatisticsTargetCompletionReqVO reqVO) {
        int year = validateCompletionPeriod(reqVO);
        Long targetScopeId = getTargetScopeId(reqVO);
        CrmPerformanceTargetTypeEnum targetType = CrmPerformanceTargetTypeEnum.fromType(reqVO.getTargetType());
        if (targetType == null) {
            throw exception(PERFORMANCE_TARGET_TYPE_INVALID);
        }

        List<Long> userIds = getUserIds(reqVO);
        reqVO.setUserIds(userIds);
        List<CrmStatisticsPerformanceRespVO> actualRows = CollUtil.isEmpty(userIds)
                ? Collections.emptyList() : getActualRows(reqVO, targetType);
        Map<String, BigDecimal> actualByMonth = convertMap(actualRows, CrmStatisticsPerformanceRespVO::getTime,
                CrmStatisticsPerformanceRespVO::getCurrentMonthCount);
        List<CrmPerformanceTargetDO> targetRows = performanceTargetMapper.selectListByScopeAndYear(
                reqVO.getScopeType(), targetScopeId, year, reqVO.getTargetType());
        Map<Integer, BigDecimal> targetByMonth = convertMap(targetRows, CrmPerformanceTargetDO::getTargetMonth,
                CrmPerformanceTargetDO::getTargetValue);

        List<CrmStatisticsTargetCompletionRespVO> monthlyList = new ArrayList<>(12);
        BigDecimal annualTarget = BigDecimal.ZERO;
        BigDecimal annualActual = BigDecimal.ZERO;
        for (int month = 1; month <= 12; month++) {
            String monthKey = String.format("%d%02d", year, month);
            BigDecimal targetValue = targetByMonth.getOrDefault(month, BigDecimal.ZERO);
            BigDecimal actualValue = actualByMonth.getOrDefault(monthKey, BigDecimal.ZERO);
            annualTarget = annualTarget.add(targetValue);
            annualActual = annualActual.add(actualValue);
            monthlyList.add(new CrmStatisticsTargetCompletionRespVO()
                    .setTime(String.format("%d-%02d", year, month))
                    .setTargetValue(targetValue.toPlainString())
                    .setActualValue(actualValue.toPlainString())
                    .setCompletionRate(calculateCompletionRate(actualValue, targetValue)));
        }
        return new CrmStatisticsTargetCompletionSummaryRespVO()
                .setTargetType(reqVO.getTargetType())
                .setAnnualTarget(annualTarget.toPlainString())
                .setAnnualActual(annualActual.toPlainString())
                .setAnnualCompletionRate(calculateCompletionRate(annualActual, annualTarget))
                .setMonthlyList(monthlyList);
    }

    private List<CrmStatisticsPerformanceRespVO> getActualRows(CrmStatisticsPerformanceReqVO reqVO,
                                                                CrmPerformanceTargetTypeEnum targetType) {
        return switch (targetType) {
            case CONTRACT_PRICE -> performanceMapper.selectContractPricePerformance(reqVO);
            case RECEIVABLE_PRICE -> performanceMapper.selectReceivablePricePerformance(reqVO);
            case FOLLOW_UP_COUNT -> performanceMapper.selectFollowUpCountPerformance(reqVO);
            case CUSTOMER_COUNT -> performanceMapper.selectCustomerCountPerformance(reqVO);
            case BUSINESS_COUNT -> performanceMapper.selectBusinessCountPerformance(reqVO);
        };
    }

    private int validateCompletionPeriod(CrmStatisticsTargetCompletionReqVO reqVO) {
        if (reqVO.getTimes() == null || reqVO.getTimes().length != 2
                || reqVO.getTimes()[0] == null || reqVO.getTimes()[1] == null) {
            throw exception(PERFORMANCE_TARGET_PERIOD_INVALID);
        }
        LocalDateTime start = reqVO.getTimes()[0];
        LocalDateTime end = reqVO.getTimes()[1];
        int year = start.getYear();
        boolean completeYear = end.getYear() == year
                && start.toLocalDate().equals(java.time.LocalDate.of(year, 1, 1))
                && start.toLocalTime().equals(LocalTime.MIDNIGHT)
                && end.toLocalDate().equals(java.time.LocalDate.of(year, 12, 31))
                && end.toLocalTime().equals(LocalTime.of(23, 59, 59));
        if (!completeYear) {
            throw exception(PERFORMANCE_TARGET_PERIOD_INVALID);
        }
        return year;
    }

    private Long getTargetScopeId(CrmStatisticsTargetCompletionReqVO reqVO) {
        CrmPerformanceTargetScopeTypeEnum scopeType = CrmPerformanceTargetScopeTypeEnum.fromType(reqVO.getScopeType());
        if (scopeType == null) {
            throw exception(PERFORMANCE_TARGET_FILTER_SCOPE_MISMATCH);
        }
        return switch (scopeType) {
            case COMPANY -> {
                DeptRespDTO dept = deptApi.getDept(reqVO.getDeptId());
                if (reqVO.getUserId() != null || dept == null || !Long.valueOf(0L).equals(dept.getParentId())) {
                    throw exception(PERFORMANCE_TARGET_FILTER_SCOPE_MISMATCH);
                }
                yield 0L;
            }
            case DEPARTMENT -> {
                if (reqVO.getUserId() != null) {
                    throw exception(PERFORMANCE_TARGET_FILTER_SCOPE_MISMATCH);
                }
                yield reqVO.getDeptId();
            }
            case USER -> {
                if (reqVO.getUserId() == null) {
                    throw exception(PERFORMANCE_TARGET_FILTER_SCOPE_MISMATCH);
                }
                yield reqVO.getUserId();
            }
        };
    }

    private static BigDecimal calculateCompletionRate(BigDecimal actualValue, BigDecimal targetValue) {
        if (targetValue.signum() == 0) {
            return null;
        }
        return actualValue.multiply(BigDecimal.valueOf(100)).divide(targetValue, 2, RoundingMode.HALF_UP);
    }

    /**
     * 获得员工业绩数据
     *
     * 1. 获得今年 + 去年的数据
     * 2. 遍历今年的月份，逐个拼接去年的月份数据
     *
     * @param performanceReqVO  参数
     * @param performanceFunction 员工业绩统计方法
     * @return 员工业绩数据
     */
    private List<CrmStatisticsPerformanceRespVO> getPerformance(CrmStatisticsPerformanceReqVO performanceReqVO,
                                                                Function<CrmStatisticsPerformanceReqVO, List<CrmStatisticsPerformanceRespVO>> performanceFunction) {

        // 1. 获得用户编号数组
        List<Long> userIds = getUserIds(performanceReqVO);
        if (CollUtil.isEmpty(userIds)) {
            return Collections.emptyList();
        }
        performanceReqVO.setUserIds(userIds);

        // 2. 获得业绩数据
        int year = performanceReqVO.getTimes()[0].getYear(); // 获取查询的年份
        LocalDateTime originalStartTime = performanceReqVO.getTimes()[0];
        List<CrmStatisticsPerformanceRespVO> performanceList;
        try {
            performanceReqVO.getTimes()[0] = originalStartTime.minusYears(1);
            performanceList = performanceFunction.apply(performanceReqVO);
        } finally {
            // Request VO 仍可能被日志、审计或后续逻辑读取，不能留下被扩展一年的查询起点
            performanceReqVO.getTimes()[0] = originalStartTime;
        }
        Map<String, BigDecimal> performanceMap = convertMap(performanceList, CrmStatisticsPerformanceRespVO::getTime,
                CrmStatisticsPerformanceRespVO::getCurrentMonthCount);

        // 3. 组装数据返回
        List<CrmStatisticsPerformanceRespVO> result = new ArrayList<>();
        for (int month = 1; month <= 12; month++) {
            String currentMonth = String.format("%d%02d", year, month);
            String lastMonth = month == 1 ? String.format("%d%02d", year - 1, 12) : String.format("%d%02d", year, month - 1);
            String lastYear = String.format("%d%02d", year - 1, month);
            BigDecimal currentValue = performanceMap.getOrDefault(currentMonth, BigDecimal.ZERO);
            BigDecimal lastMonthValue = performanceMap.getOrDefault(lastMonth, BigDecimal.ZERO);
            BigDecimal lastYearValue = performanceMap.getOrDefault(lastYear, BigDecimal.ZERO);
            result.add(new CrmStatisticsPerformanceRespVO().setTime(currentMonth)
                    .setCurrentMonthCount(currentValue)
                    .setLastMonthCount(lastMonthValue)
                    .setLastYearCount(lastYearValue)
                    .setMonthOnMonthRate(calculateGrowthRate(currentValue, lastMonthValue))
                    .setYearOnYearRate(calculateGrowthRate(currentValue, lastYearValue)));
        }
        return result;
    }

    static BigDecimal calculateGrowthRate(BigDecimal currentValue, BigDecimal previousValue) {
        if (currentValue == null || previousValue == null || previousValue.signum() == 0) {
            return null;
        }
        return currentValue.subtract(previousValue)
                .multiply(BigDecimal.valueOf(100))
                .divide(previousValue, 2, RoundingMode.HALF_UP);
    }

    /**
     * 获取用户编号数组。如果用户编号为空, 则获得部门下的用户编号数组，包括子部门的所有用户编号
     *
     * @param reqVO 请求参数
     * @return 用户编号数组
     */
    private List<Long> getUserIds(CrmStatisticsPerformanceReqVO reqVO) {
        // 情况一：选中某个用户
        if (ObjUtil.isNotNull(reqVO.getUserId())) {
            return ListUtil.of(reqVO.getUserId());
        }
        // 情况二：选中某个部门
        // 2.1 获得部门列表
        final Long deptId = reqVO.getDeptId();
        List<Long> deptIds = convertList(deptApi.getChildDeptList(deptId), DeptRespDTO::getId);
        deptIds.add(deptId);
        // 2.2 获得用户编号
        return convertList(adminUserApi.getUserListByDeptIds(deptIds), AdminUserRespDTO::getId);
    }

}
