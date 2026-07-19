package com.meession.etm.module.crm.controller.admin.statistics;

import com.meession.etm.framework.common.pojo.CommonResult;
import com.meession.etm.module.crm.controller.admin.statistics.vo.metadata.CrmStatisticsMetadataCatalogRespVO;
import com.meession.etm.module.crm.service.statistics.CrmStatisticsMetadataService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.constraints.Pattern;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import static com.meession.etm.framework.common.pojo.CommonResult.success;

@Tag(name = "管理后台 - CRM 统计指标元数据")
@RestController
@RequestMapping("/crm/statistics-metadata")
@Validated
public class CrmStatisticsMetadataController {

    private static final String SCOPE_PATTERN = "customer|funnel|performance|portrait|rank|workorder";

    @Resource
    private CrmStatisticsMetadataService metadataService;

    @GetMapping("/catalog")
    @Operation(summary = "获得统计指标血缘目录", description = "返回当前统计域的来源、业务时间、过滤、权限和刷新口径")
    @Parameter(name = "scope", description = "统计域", required = true, example = "funnel")
    @PreAuthorize("(#scope == 'customer' && @ss.hasPermission('crm:statistics-customer:query'))"
            + " || (#scope == 'funnel' && @ss.hasPermission('crm:statistics-funnel:query'))"
            + " || (#scope == 'performance' && @ss.hasPermission('crm:statistics-performance:query'))"
            + " || (#scope == 'portrait' && @ss.hasPermission('crm:statistics-portrait:query'))"
            + " || (#scope == 'rank' && @ss.hasPermission('crm:statistics-rank:query'))"
            + " || (#scope == 'workorder' && @ss.hasPermission('crm:statistics-work-order:query'))")
    public CommonResult<CrmStatisticsMetadataCatalogRespVO> getCatalog(
            @RequestParam @Pattern(regexp = SCOPE_PATTERN) String scope) {
        return success(metadataService.getCatalog(scope));
    }
}
