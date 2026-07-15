package com.meession.etm.module.crm.controller.admin.statistics;

import cn.hutool.extra.spring.SpringUtil;
import com.meession.etm.framework.common.pojo.CommonResult;
import com.meession.etm.framework.common.pojo.PageResult;
import com.meession.etm.module.crm.controller.admin.customer.CrmCustomerController;
import com.meession.etm.module.crm.controller.admin.customer.vo.customer.CrmCustomerRespVO;
import com.meession.etm.module.crm.controller.admin.statistics.vo.portrait.CrmStatisticsPortraitReqVO;
import com.meession.etm.module.crm.controller.admin.statistics.vo.portrait.CrmStatisticsPortraitCustomerPageReqVO;
import com.meession.etm.module.crm.controller.admin.statistics.vo.portrait.CrmStatisticCustomerAreaRespVO;
import com.meession.etm.module.crm.controller.admin.statistics.vo.portrait.CrmStatisticCustomerDealStatusRespVO;
import com.meession.etm.module.crm.controller.admin.statistics.vo.portrait.CrmStatisticCustomerIndustryRespVO;
import com.meession.etm.module.crm.controller.admin.statistics.vo.portrait.CrmStatisticCustomerLevelRespVO;
import com.meession.etm.module.crm.controller.admin.statistics.vo.portrait.CrmStatisticCustomerSourceRespVO;
import com.meession.etm.module.crm.service.statistics.CrmStatisticsPortraitService;
import com.meession.etm.module.crm.framework.permission.core.annotations.CrmStatisticsDataScope;
import com.meession.etm.module.crm.dal.dataobject.customer.CrmCustomerDO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

import static com.meession.etm.framework.common.pojo.CommonResult.success;

@Tag(name = "管理后台 - CRM 客户画像")
@RestController
@RequestMapping("/crm/statistics-portrait")
@Validated
@CrmStatisticsDataScope
public class CrmStatisticsPortraitController {

    @Resource
    private CrmStatisticsPortraitService statisticsPortraitService;

    @GetMapping("/get-customer-area-summary")
    @Operation(summary = "获取客户省份统计数据", description = "用于【按省份分布】页面")
    @PreAuthorize("@ss.hasPermission('crm:statistics-portrait:query')")
    public CommonResult<List<CrmStatisticCustomerAreaRespVO>> getCustomerAreaSummary(@Valid CrmStatisticsPortraitReqVO reqVO) {
        return success(statisticsPortraitService.getCustomerSummaryByArea(reqVO));
    }

    @GetMapping("/get-customer-city-summary")
    @Operation(summary = "获取客户城市统计数据", description = "用于【所在城市】页面")
    @PreAuthorize("@ss.hasPermission('crm:statistics-portrait:query')")
    public CommonResult<List<CrmStatisticCustomerAreaRespVO>> getCustomerCitySummary(
            @Valid CrmStatisticsPortraitReqVO reqVO) {
        return success(statisticsPortraitService.getCustomerSummaryByCity(reqVO));
    }

    @GetMapping("/get-customer-country-summary")
    @Operation(summary = "获取客户国家统计数据", description = "用于【按国家分布】页面")
    @PreAuthorize("@ss.hasPermission('crm:statistics-portrait:query')")
    public CommonResult<List<CrmStatisticCustomerAreaRespVO>> getCustomerCountrySummary(
            @Valid CrmStatisticsPortraitReqVO reqVO) {
        return success(statisticsPortraitService.getCustomerSummaryByCountry(reqVO));
    }

    @GetMapping("/get-customer-page-by-area")
    @Operation(summary = "获取区域客户明细分页", description = "按统计时间、负责人范围和区域下级节点钻取")
    @PreAuthorize("@ss.hasPermission('crm:statistics-portrait:query') " +
            "and @ss.hasPermission('crm:customer:query')")
    public CommonResult<PageResult<CrmCustomerRespVO>> getCustomerPageByArea(
            @Valid CrmStatisticsPortraitCustomerPageReqVO reqVO) {
        PageResult<CrmCustomerDO> pageResult = statisticsPortraitService.getCustomerPageByArea(reqVO);
        List<CrmCustomerRespVO> list = SpringUtil.getBean(CrmCustomerController.class)
                .buildCustomerDetailList(pageResult.getList());
        return success(new PageResult<>(list, pageResult.getTotal()));
    }

    @GetMapping("/get-customer-industry-summary")
    @Operation(summary = "获取客户行业统计数据", description = "用于【客户行业分析】页面")
    @PreAuthorize("@ss.hasPermission('crm:statistics-portrait:query')")
    public CommonResult<List<CrmStatisticCustomerIndustryRespVO>> getCustomerIndustrySummary(@Valid CrmStatisticsPortraitReqVO reqVO) {
        return success(statisticsPortraitService.getCustomerSummaryByIndustry(reqVO));
    }

    @GetMapping("/get-customer-level-summary")
    @Operation(summary = "获取客户级别统计数据", description = "用于【客户级别分析】页面")
    @PreAuthorize("@ss.hasPermission('crm:statistics-portrait:query')")
    public CommonResult<List<CrmStatisticCustomerLevelRespVO>> getCustomerLevelSummary(@Valid CrmStatisticsPortraitReqVO reqVO) {
        return success(statisticsPortraitService.getCustomerSummaryByLevel(reqVO));
    }

    @GetMapping("/get-customer-source-summary")
    @Operation(summary = "获取客户来源统计数据", description = "用于【客户来源分析】页面")
    @PreAuthorize("@ss.hasPermission('crm:statistics-portrait:query')")
    public CommonResult<List<CrmStatisticCustomerSourceRespVO>> getCustomerSourceSummary(@Valid CrmStatisticsPortraitReqVO reqVO) {
        return success(statisticsPortraitService.getCustomerSummaryBySource(reqVO));
    }

    @GetMapping("/get-customer-deal-status-summary")
    @Operation(summary = "获取客户成交状态分布", description = "用于【客户状态分析】页面")
    @PreAuthorize("@ss.hasPermission('crm:statistics-portrait:query')")
    public CommonResult<List<CrmStatisticCustomerDealStatusRespVO>> getCustomerDealStatusSummary(
            @Valid CrmStatisticsPortraitReqVO reqVO) {
        return success(statisticsPortraitService.getCustomerSummaryByDealStatus(reqVO));
    }

}
