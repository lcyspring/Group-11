package com.meession.etm.module.crm.controller.admin.statistics;

import com.meession.etm.framework.common.pojo.CommonResult;
import com.meession.etm.module.crm.controller.admin.statistics.vo.performance.CrmPerformanceTargetBaseReqVO;
import com.meession.etm.module.crm.controller.admin.statistics.vo.performance.CrmPerformanceTargetListReqVO;
import com.meession.etm.module.crm.controller.admin.statistics.vo.performance.CrmPerformanceTargetRespVO;
import com.meession.etm.module.crm.controller.admin.statistics.vo.performance.CrmPerformanceTargetSaveReqVO;
import com.meession.etm.module.crm.dal.dataobject.statistics.CrmPerformanceTargetDO;
import com.meession.etm.module.crm.service.statistics.CrmPerformanceTargetService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static com.meession.etm.framework.common.pojo.CommonResult.success;

@Tag(name = "管理后台 - CRM 业绩目标")
@RestController
@RequestMapping("/crm/performance-target")
@Validated
public class CrmPerformanceTargetController {

    @Resource
    private CrmPerformanceTargetService performanceTargetService;

    @PutMapping("/save")
    @Operation(summary = "新增或修改年度业绩目标", description = "一次保存 12 个月目标，年度和季度值自动汇总")
    @PreAuthorize("@ss.hasPermission('crm:performance-target:update')")
    public CommonResult<Boolean> savePerformanceTarget(@Valid @RequestBody CrmPerformanceTargetSaveReqVO reqVO) {
        performanceTargetService.savePerformanceTarget(reqVO);
        return success(true);
    }

    @DeleteMapping("/delete")
    @Operation(summary = "删除年度业绩目标")
    @PreAuthorize("@ss.hasPermission('crm:performance-target:delete')")
    public CommonResult<Boolean> deletePerformanceTarget(@Valid CrmPerformanceTargetBaseReqVO reqVO) {
        performanceTargetService.deletePerformanceTarget(reqVO);
        return success(true);
    }

    @GetMapping("/list")
    @Operation(summary = "获得指定范围和年度的业绩目标")
    @PreAuthorize("@ss.hasPermission('crm:statistics-performance:query')")
    public CommonResult<List<CrmPerformanceTargetRespVO>> getPerformanceTargetList(
            @Valid CrmPerformanceTargetListReqVO reqVO) {
        List<CrmPerformanceTargetDO> rows = performanceTargetService.getPerformanceTargetList(reqVO);
        Map<Integer, List<CrmPerformanceTargetDO>> rowsByType = new LinkedHashMap<>();
        rows.forEach(row -> rowsByType.computeIfAbsent(row.getTargetType(), key -> new ArrayList<>()).add(row));
        return success(rowsByType.entrySet().stream()
                .map(entry -> buildResponse(reqVO, entry.getKey(), entry.getValue()))
                .toList());
    }

    private static CrmPerformanceTargetRespVO buildResponse(CrmPerformanceTargetListReqVO reqVO, Integer targetType,
                                                             List<CrmPerformanceTargetDO> rows) {
        Map<Integer, BigDecimal> valueByMonth = new LinkedHashMap<>();
        rows.forEach(row -> valueByMonth.put(row.getTargetMonth(), row.getTargetValue()));
        List<BigDecimal> monthlyTargets = new ArrayList<>(12);
        for (int month = 1; month <= 12; month++) {
            monthlyTargets.add(valueByMonth.getOrDefault(month, BigDecimal.ZERO));
        }
        List<BigDecimal> quarterlyTargets = new ArrayList<>(4);
        for (int quarter = 0; quarter < 4; quarter++) {
            int start = quarter * 3;
            quarterlyTargets.add(monthlyTargets.subList(start, start + 3).stream()
                    .reduce(BigDecimal.ZERO, BigDecimal::add));
        }
        return new CrmPerformanceTargetRespVO()
                .setScopeType(reqVO.getScopeType())
                .setScopeId(reqVO.getScopeId())
                .setTargetYear(reqVO.getTargetYear())
                .setTargetType(targetType)
                .setMonthlyTargets(monthlyTargets.stream().map(BigDecimal::toPlainString).toList())
                .setQuarterlyTargets(quarterlyTargets.stream().map(BigDecimal::toPlainString).toList())
                .setAnnualTarget(monthlyTargets.stream().reduce(BigDecimal.ZERO, BigDecimal::add).toPlainString());
    }

}
