package com.meession.etm.module.crm.service.statistics;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.collection.ListUtil;
import cn.hutool.core.util.ObjUtil;
import com.meession.etm.module.crm.controller.admin.statistics.vo.win.CrmStatisticsWinAnalysisReqVO;
import com.meession.etm.module.crm.controller.admin.statistics.vo.win.CrmStatisticsWinAnalysisRespVO;
import com.meession.etm.module.crm.dal.mysql.statistics.CrmStatisticsWinAnalysisMapper;
import com.meession.etm.module.system.api.dept.DeptApi;
import com.meession.etm.module.system.api.dept.dto.DeptRespDTO;
import com.meession.etm.module.system.api.user.AdminUserApi;
import com.meession.etm.module.system.api.user.dto.AdminUserRespDTO;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static com.meession.etm.framework.common.util.collection.CollectionUtils.convertList;

@Service
public class CrmStatisticsWinAnalysisServiceImpl implements CrmStatisticsWinAnalysisService {

    @Resource
    private CrmStatisticsWinAnalysisMapper winAnalysisMapper;

    @Resource
    private AdminUserApi adminUserApi;

    @Resource
    private DeptApi deptApi;

    @Override
    public CrmStatisticsWinAnalysisRespVO getWinAnalysis(CrmStatisticsWinAnalysisReqVO reqVO) {
        List<Long> userIds = getUserIds(reqVO);
        if (CollUtil.isEmpty(userIds)) {
            return buildEmptyResult();
        }
        reqVO.setUserIds(userIds);

        Map<String, Object> summary = winAnalysisMapper.selectBusinessWinSummary(reqVO);
        Long totalCount = ((Number) summary.get("total_count")).longValue();
        Long winCount = ((Number) summary.get("win_count")).longValue();
        BigDecimal winAmount = (BigDecimal) summary.get("win_amount");

        BigDecimal winRate = totalCount > 0 ? BigDecimal.valueOf(winCount).multiply(BigDecimal.valueOf(100))
                .divide(BigDecimal.valueOf(totalCount), 2, RoundingMode.HALF_UP) : BigDecimal.ZERO;
        BigDecimal avgWinAmount = winCount > 0 ? winAmount.divide(BigDecimal.valueOf(winCount), 2, RoundingMode.HALF_UP) : BigDecimal.ZERO;

        List<CrmStatisticsWinAnalysisRespVO.WinRateByDateItem> winRateByDate = winAnalysisMapper.selectWinRateByDate(reqVO);
        List<CrmStatisticsWinAnalysisRespVO.WinAmountByIndustryItem> winAmountByIndustry = winAnalysisMapper.selectWinAmountByIndustry(reqVO);
        List<CrmStatisticsWinAnalysisRespVO.WinAmountByCustomerLevelItem> winAmountByCustomerLevel = winAnalysisMapper.selectWinAmountByCustomerLevel(reqVO);

        return CrmStatisticsWinAnalysisRespVO.builder()
                .winRate(winRate)
                .totalBusinessCount(totalCount)
                .winBusinessCount(winCount)
                .totalWinAmount(winAmount)
                .avgWinAmount(avgWinAmount)
                .winRateByDate(winRateByDate)
                .winAmountByIndustry(winAmountByIndustry)
                .winAmountByCustomerLevel(winAmountByCustomerLevel)
                .build();
    }

    private CrmStatisticsWinAnalysisRespVO buildEmptyResult() {
        return CrmStatisticsWinAnalysisRespVO.builder()
                .winRate(BigDecimal.ZERO)
                .totalBusinessCount(0L)
                .winBusinessCount(0L)
                .totalWinAmount(BigDecimal.ZERO)
                .avgWinAmount(BigDecimal.ZERO)
                .winRateByDate(Collections.emptyList())
                .winAmountByIndustry(Collections.emptyList())
                .winAmountByCustomerLevel(Collections.emptyList())
                .build();
    }

    private List<Long> getUserIds(CrmStatisticsWinAnalysisReqVO reqVO) {
        if (ObjUtil.isNotNull(reqVO.getUserId())) {
            return ListUtil.of(reqVO.getUserId());
        }
        List<Long> deptIds = convertList(deptApi.getChildDeptList(reqVO.getDeptId()), DeptRespDTO::getId);
        deptIds.add(reqVO.getDeptId());
        return convertList(adminUserApi.getUserListByDeptIds(deptIds), AdminUserRespDTO::getId);
    }

}