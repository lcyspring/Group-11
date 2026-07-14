package com.meession.etm.module.crm.service.statistics;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.collection.ListUtil;
import cn.hutool.core.util.ObjUtil;
import com.meession.etm.framework.ip.core.Area;
import com.meession.etm.framework.ip.core.enums.AreaTypeEnum;
import com.meession.etm.framework.ip.core.utils.AreaUtils;
import com.meession.etm.module.crm.controller.admin.statistics.vo.portrait.*;
import com.meession.etm.module.crm.dal.mysql.statistics.CrmStatisticsPortraitMapper;
import com.meession.etm.module.system.api.dept.DeptApi;
import com.meession.etm.module.system.api.dept.dto.DeptRespDTO;
import com.meession.etm.module.system.api.user.AdminUserApi;
import com.meession.etm.module.system.api.user.dto.AdminUserRespDTO;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.meession.etm.framework.common.util.collection.CollectionUtils.convertList;

/**
 * CRM 客户画像 Service 实现类
 *
 * @author HUIHUI
 */
@Service
public class CrmStatisticsPortraitServiceImpl implements CrmStatisticsPortraitService {

    @Resource
    private CrmStatisticsPortraitMapper portraitMapper;

    @Resource
    private AdminUserApi adminUserApi;
    @Resource
    private DeptApi deptApi;

    @Override
    public List<CrmStatisticCustomerAreaRespVO> getCustomerSummaryByArea(CrmStatisticsPortraitReqVO reqVO) {
        return getCustomerSummaryByAreaType(reqVO, AreaTypeEnum.PROVINCE);
    }

    @Override
    public List<CrmStatisticCustomerAreaRespVO> getCustomerSummaryByCity(CrmStatisticsPortraitReqVO reqVO) {
        return getCustomerSummaryByAreaType(reqVO, AreaTypeEnum.CITY);
    }

    @Override
    public List<CrmStatisticCustomerAreaRespVO> getCustomerSummaryByCountry(CrmStatisticsPortraitReqVO reqVO) {
        return getCustomerSummaryByAreaType(reqVO, AreaTypeEnum.COUNTRY);
    }

    private List<CrmStatisticCustomerAreaRespVO> getCustomerSummaryByAreaType(
            CrmStatisticsPortraitReqVO reqVO, AreaTypeEnum areaType) {
        // 1. 获得用户编号数组
        List<Long> userIds = getUserIds(reqVO);
        if (CollUtil.isEmpty(userIds)) {
            return Collections.emptyList();
        }
        reqVO.setUserIds(userIds);

        // 2. 获取客户地区统计数据
        List<CrmStatisticCustomerAreaRespVO> list = portraitMapper.selectSummaryListGroupByAreaId(reqVO);
        if (CollUtil.isEmpty(list)) {
            return Collections.emptyList();
        }

        // 3. 原始 SQL 按客户保存的最细区域分组；转换到城市/省份/国家后必须再次聚合。
        Map<Integer, CrmStatisticCustomerAreaRespVO> summaries = new HashMap<>();
        for (CrmStatisticCustomerAreaRespVO item : list) {
            Integer summaryAreaId = AreaUtils.getParentIdByType(item.getAreaId(), areaType);
            Area summaryArea = AreaUtils.getArea(summaryAreaId);
            CrmStatisticCustomerAreaRespVO summary = summaries.computeIfAbsent(summaryAreaId,
                    ignored -> new CrmStatisticCustomerAreaRespVO()
                            .setAreaId(summaryAreaId)
                            .setAreaName(summaryArea == null ? "未知" : summaryArea.getName())
                            .setCustomerCount(0)
                            .setDealCount(0));
            summary.setCustomerCount(summary.getCustomerCount() + zeroIfNull(item.getCustomerCount()));
            summary.setDealCount(summary.getDealCount() + zeroIfNull(item.getDealCount()));
        }
        return summaries.values().stream()
                .sorted(Comparator.comparingInt(CrmStatisticCustomerAreaRespVO::getCustomerCount).reversed()
                        .thenComparing(item -> item.getAreaId() == null ? Integer.MAX_VALUE : item.getAreaId()))
                .toList();
    }

    private static int zeroIfNull(Integer value) {
        return value == null ? 0 : value;
    }

    @Override
    public List<CrmStatisticCustomerIndustryRespVO> getCustomerSummaryByIndustry(CrmStatisticsPortraitReqVO reqVO) {
        // 1. 获得用户编号数组
        List<Long> userIds = getUserIds(reqVO);
        if (CollUtil.isEmpty(userIds)) {
            return Collections.emptyList();
        }
        reqVO.setUserIds(userIds);

        // 2. 获取客户行业统计数据
        return portraitMapper.selectCustomerIndustryListGroupByIndustryId(reqVO);
    }

    @Override
    public List<CrmStatisticCustomerSourceRespVO> getCustomerSummaryBySource(CrmStatisticsPortraitReqVO reqVO) {
        // 1. 获得用户编号数组
        List<Long> userIds = getUserIds(reqVO);
        if (CollUtil.isEmpty(userIds)) {
            return Collections.emptyList();
        }
        reqVO.setUserIds(userIds);

        // 2. 获取客户行业统计数据
        return portraitMapper.selectCustomerSourceListGroupBySource(reqVO);
    }

    @Override
    public List<CrmStatisticCustomerLevelRespVO> getCustomerSummaryByLevel(CrmStatisticsPortraitReqVO reqVO) {
        // 1. 获得用户编号数组
        List<Long> userIds = getUserIds(reqVO);
        if (CollUtil.isEmpty(userIds)) {
            return Collections.emptyList();
        }
        reqVO.setUserIds(userIds);

        // 2. 获取客户级别统计数据
        return portraitMapper.selectCustomerLevelListGroupByLevel(reqVO);
    }

    @Override
    public List<CrmStatisticCustomerDealStatusRespVO> getCustomerSummaryByDealStatus(CrmStatisticsPortraitReqVO reqVO) {
        List<Long> userIds = getUserIds(reqVO);
        if (CollUtil.isEmpty(userIds)) {
            return Collections.emptyList();
        }
        reqVO.setUserIds(userIds);
        return portraitMapper.selectCustomerDealStatusList(reqVO);
    }

    /**
     * 获取用户编号数组。如果用户编号为空, 则获得部门下的用户编号数组，包括子部门的所有用户编号
     *
     * @param reqVO 请求参数
     * @return 用户编号数组
     */
    private List<Long> getUserIds(CrmStatisticsPortraitReqVO reqVO) {
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
