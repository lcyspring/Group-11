package com.meession.etm.module.crm.controller.admin.statistics;

import com.meession.etm.framework.common.pojo.CommonResult;
import com.meession.etm.module.crm.controller.admin.statistics.vo.win.CrmStatisticsWinAnalysisReqVO;
import com.meession.etm.module.crm.controller.admin.statistics.vo.win.CrmStatisticsWinAnalysisRespVO;
import com.meession.etm.module.crm.service.statistics.CrmStatisticsWinAnalysisService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static com.meession.etm.framework.common.pojo.CommonResult.success;

@Tag(name = "管理后台 - CRM 商机赢单分析")
@RestController
@RequestMapping("/crm/statistics/win-analysis")
public class CrmStatisticsWinAnalysisController {

    @Resource
    private CrmStatisticsWinAnalysisService winAnalysisService;

    @PostMapping("/get")
    @Operation(summary = "获取商机赢单分析数据")
    public CommonResult<CrmStatisticsWinAnalysisRespVO> getWinAnalysis(@Valid @RequestBody CrmStatisticsWinAnalysisReqVO reqVO) {
        return success(winAnalysisService.getWinAnalysis(reqVO));
    }

}