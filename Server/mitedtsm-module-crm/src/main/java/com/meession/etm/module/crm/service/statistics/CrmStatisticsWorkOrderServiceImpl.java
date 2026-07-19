package com.meession.etm.module.crm.service.statistics;

import cn.hutool.core.collection.CollUtil;
import com.meession.etm.framework.common.util.date.LocalDateTimeUtils;
import com.meession.etm.framework.tenant.core.context.TenantContextHolder;
import com.meession.etm.module.crm.controller.admin.statistics.vo.workorder.*;
import com.meession.etm.module.crm.dal.mysql.statistics.CrmStatisticsWorkOrderMapper;
import com.meession.etm.module.system.api.user.AdminUserApi;
import com.meession.etm.module.system.api.user.dto.AdminUserRespDTO;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static com.meession.etm.framework.common.util.collection.CollectionUtils.convertList;
import static com.meession.etm.framework.common.util.collection.CollectionUtils.convertSet;

@Service
@Validated
public class CrmStatisticsWorkOrderServiceImpl implements CrmStatisticsWorkOrderService {

    @Resource
    private CrmStatisticsWorkOrderMapper mapper;
    @Resource
    private AdminUserApi adminUserApi;

    @Override
    public CrmStatisticsWorkOrderSummaryRespVO getSummary(CrmStatisticsWorkOrderReqVO reqVO, Long userId, boolean queryAll) {
        CrmStatisticsWorkOrderSummaryRespVO summary = mapper.selectSummary(reqVO.getTimes(), tenantId(), userId, queryAll);
        if (summary == null) {
            summary = new CrmStatisticsWorkOrderSummaryRespVO();
        }
        long total = value(summary.getTotalCount());
        long completed = value(summary.getCompletedCount());
        summary.setTotalCount(total).setPendingCount(value(summary.getPendingCount()))
                .setProcessingCount(value(summary.getProcessingCount())).setCompletedCount(completed)
                .setReturnedCount(value(summary.getReturnedCount()))
                .setCompletionRate(total == 0 ? "0%" : BigDecimal.valueOf(completed * 100D)
                        .divide(BigDecimal.valueOf(total), 2, RoundingMode.HALF_UP).stripTrailingZeros().toPlainString() + "%");
        return summary;
    }

    @Override
    public List<CrmStatisticsWorkOrderStatusRespVO> getByStatus(CrmStatisticsWorkOrderReqVO reqVO, Long userId, boolean queryAll) {
        return mapper.selectByStatus(reqVO.getTimes(), tenantId(), userId, queryAll);
    }

    @Override
    public List<CrmStatisticsWorkOrderTypeRespVO> getByType(CrmStatisticsWorkOrderReqVO reqVO, Long userId, boolean queryAll) {
        return mapper.selectByType(reqVO.getTimes(), tenantId(), userId, queryAll);
    }

    @Override
    public List<CrmStatisticsWorkOrderHandlerRespVO> getByHandler(CrmStatisticsWorkOrderReqVO reqVO, Long userId, boolean queryAll) {
        List<CrmStatisticsWorkOrderHandlerRespVO> rows = mapper.selectByHandler(reqVO.getTimes(), tenantId(), userId, queryAll);
        if (CollUtil.isEmpty(rows)) return Collections.emptyList();
        Map<Long, AdminUserRespDTO> users = adminUserApi.getUserMap(convertSet(rows, CrmStatisticsWorkOrderHandlerRespVO::getHandlerUserId));
        rows.forEach(row -> {
            AdminUserRespDTO user = users.get(row.getHandlerUserId());
            if (user != null) row.setHandlerUserName(user.getNickname());
        });
        return rows;
    }

    @Override
    public List<CrmStatisticsWorkOrderTrendRespVO> getTrend(CrmStatisticsWorkOrderReqVO reqVO, Long userId, boolean queryAll) {
        List<CrmStatisticsWorkOrderTrendRespVO> rows = mapper.selectTrend(reqVO.getTimes(), tenantId(), userId, queryAll);
        List<LocalDateTime[]> ranges = LocalDateTimeUtils.getDateRangeList(reqVO.getTimes()[0], reqVO.getTimes()[1], reqVO.getInterval());
        return convertList(ranges, range -> {
            long created = rows.stream().filter(row -> inRange(range, row, true)).mapToLong(row -> value(row.getCreatedCount())).sum();
            long completed = rows.stream().filter(row -> inRange(range, row, false)).mapToLong(row -> value(row.getCompletedCount())).sum();
            return new CrmStatisticsWorkOrderTrendRespVO().setTime(LocalDateTimeUtils.formatDateRange(range[0], range[1], reqVO.getInterval()))
                    .setCreatedCount(created).setCompletedCount(completed);
        });
    }

    private static boolean inRange(LocalDateTime[] range, CrmStatisticsWorkOrderTrendRespVO row, boolean created) {
        if (row.getTime() == null) return false;
        LocalDateTime time = LocalDateTime.parse(row.getTime());
        return !time.isBefore(range[0]) && !time.isAfter(range[1])
                && (created ? value(row.getCreatedCount()) > 0 : value(row.getCompletedCount()) > 0);
    }

    private static long value(Long value) { return value == null ? 0 : value; }

    private static Long tenantId() { return TenantContextHolder.getRequiredTenantId(); }
}
