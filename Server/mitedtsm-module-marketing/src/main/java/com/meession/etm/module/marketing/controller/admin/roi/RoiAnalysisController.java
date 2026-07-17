package com.meession.etm.module.marketing.controller.admin.roi;

import com.meession.etm.framework.common.pojo.CommonResult;
import com.meession.etm.module.marketing.controller.admin.roi.vo.RoiAnalysisCampaignRankingRespVO;
import com.meession.etm.module.marketing.controller.admin.roi.vo.RoiAnalysisChannelRespVO;
import com.meession.etm.module.marketing.controller.admin.roi.vo.RoiAnalysisFunnelRespVO;
import com.meession.etm.module.marketing.controller.admin.roi.vo.RoiAnalysisReqVO;
import com.meession.etm.module.marketing.controller.admin.roi.vo.RoiAnalysisSummaryRespVO;
import com.meession.etm.module.marketing.controller.admin.roi.vo.RoiAnalysisTrendRespVO;
import com.meession.etm.module.marketing.service.roi.RoiAnalysisService;
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

@Tag(name = "管理后台 - 营销 ROI 分析")
@RestController
@RequestMapping("/marketing/roi-analysis")
@Validated
public class RoiAnalysisController {

    @Resource
    private RoiAnalysisService roiAnalysisService;

    @GetMapping("/summary")
    @Operation(summary = "获得营销 ROI 概览")
    @PreAuthorize("@ss.hasPermission('marketing:roi-analysis:query')")
    public CommonResult<RoiAnalysisSummaryRespVO> getSummary(@Valid RoiAnalysisReqVO reqVO) {
        return success(roiAnalysisService.getSummary(reqVO));
    }

    @GetMapping("/trend")
    @Operation(summary = "获得营销 ROI 趋势")
    @PreAuthorize("@ss.hasPermission('marketing:roi-analysis:query')")
    public CommonResult<List<RoiAnalysisTrendRespVO>> getTrend(@Valid RoiAnalysisReqVO reqVO) {
        return success(roiAnalysisService.getTrend(reqVO));
    }

    @GetMapping("/campaign-ranking")
    @Operation(summary = "获得营销活动 ROI 排行")
    @PreAuthorize("@ss.hasPermission('marketing:roi-analysis:query')")
    public CommonResult<List<RoiAnalysisCampaignRankingRespVO>> getCampaignRanking(@Valid RoiAnalysisReqVO reqVO) {
        return success(roiAnalysisService.getCampaignRanking(reqVO));
    }

    @GetMapping("/channel")
    @Operation(summary = "获得营销渠道 ROI 分析")
    @PreAuthorize("@ss.hasPermission('marketing:roi-analysis:query')")
    public CommonResult<List<RoiAnalysisChannelRespVO>> getChannel(@Valid RoiAnalysisReqVO reqVO) {
        return success(roiAnalysisService.getChannel(reqVO));
    }

    @GetMapping("/funnel")
    @Operation(summary = "获得营销 ROI 转化漏斗")
    @PreAuthorize("@ss.hasPermission('marketing:roi-analysis:query')")
    public CommonResult<RoiAnalysisFunnelRespVO> getFunnel(@Valid RoiAnalysisReqVO reqVO) {
        return success(roiAnalysisService.getFunnel(reqVO));
    }

}
