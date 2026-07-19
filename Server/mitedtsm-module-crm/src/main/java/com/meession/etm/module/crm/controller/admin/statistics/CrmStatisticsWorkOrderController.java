package com.meession.etm.module.crm.controller.admin.statistics;

import com.meession.etm.framework.common.pojo.CommonResult;
import com.meession.etm.module.crm.controller.admin.statistics.vo.workorder.*;
import com.meession.etm.module.crm.service.statistics.CrmStatisticsWorkOrderService;
import com.meession.etm.framework.security.core.service.SecurityFrameworkService;
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
import static com.meession.etm.framework.security.core.util.SecurityFrameworkUtils.getLoginUserId;

@Tag(name = "管理后台 - CRM 工单统计")
@RestController
@RequestMapping("/crm/statistics-work-order")
@Validated
public class CrmStatisticsWorkOrderController {
    @Resource private CrmStatisticsWorkOrderService service;
    @Resource private SecurityFrameworkService securityFrameworkService;

    @GetMapping("/summary")
    @Operation(summary = "获取工单统计汇总")
    @PreAuthorize("@ss.hasPermission('crm:statistics-work-order:query')")
    public CommonResult<CrmStatisticsWorkOrderSummaryRespVO> summary(@Valid CrmStatisticsWorkOrderReqVO reqVO) {
        return success(service.getSummary(reqVO, getLoginUserId(), queryAll()));
    }

    @GetMapping("/by-status")
    @Operation(summary = "按状态统计工单")
    @PreAuthorize("@ss.hasPermission('crm:statistics-work-order:query')")
    public CommonResult<List<CrmStatisticsWorkOrderStatusRespVO>> byStatus(@Valid CrmStatisticsWorkOrderReqVO reqVO) {
        return success(service.getByStatus(reqVO, getLoginUserId(), queryAll()));
    }

    @GetMapping("/by-type")
    @Operation(summary = "按类型统计工单")
    @PreAuthorize("@ss.hasPermission('crm:statistics-work-order:query')")
    public CommonResult<List<CrmStatisticsWorkOrderTypeRespVO>> byType(@Valid CrmStatisticsWorkOrderReqVO reqVO) {
        return success(service.getByType(reqVO, getLoginUserId(), queryAll()));
    }

    @GetMapping("/by-handler")
    @Operation(summary = "按处理人统计工单")
    @PreAuthorize("@ss.hasPermission('crm:statistics-work-order:query')")
    public CommonResult<List<CrmStatisticsWorkOrderHandlerRespVO>> byHandler(@Valid CrmStatisticsWorkOrderReqVO reqVO) {
        return success(service.getByHandler(reqVO, getLoginUserId(), queryAll()));
    }

    @GetMapping("/trend")
    @Operation(summary = "按时间统计工单创建与完结")
    @PreAuthorize("@ss.hasPermission('crm:statistics-work-order:query')")
    public CommonResult<List<CrmStatisticsWorkOrderTrendRespVO>> trend(@Valid CrmStatisticsWorkOrderReqVO reqVO) {
        return success(service.getTrend(reqVO, getLoginUserId(), queryAll()));
    }

    private boolean queryAll() {
        return securityFrameworkService.hasPermission("crm:work-order:query-all");
    }
}
