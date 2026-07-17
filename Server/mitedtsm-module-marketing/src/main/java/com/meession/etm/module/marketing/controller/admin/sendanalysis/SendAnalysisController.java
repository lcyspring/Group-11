package com.meession.etm.module.marketing.controller.admin.sendanalysis;

import com.meession.etm.framework.common.pojo.CommonResult;
import com.meession.etm.module.marketing.controller.admin.sendanalysis.vo.SendAnalysisChannelRespVO;
import com.meession.etm.module.marketing.controller.admin.sendanalysis.vo.SendAnalysisFailReasonRespVO;
import com.meession.etm.module.marketing.controller.admin.sendanalysis.vo.SendAnalysisReqVO;
import com.meession.etm.module.marketing.controller.admin.sendanalysis.vo.SendAnalysisSummaryRespVO;
import com.meession.etm.module.marketing.controller.admin.sendanalysis.vo.SendAnalysisTemplateRespVO;
import com.meession.etm.module.marketing.controller.admin.sendanalysis.vo.SendAnalysisTrendRespVO;
import com.meession.etm.module.marketing.service.sendanalysis.SendAnalysisService;
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

@Tag(name = "管理后台 - 营销发送分析")
@RestController
@RequestMapping("/marketing/send-analysis")
@Validated
public class SendAnalysisController {

    @Resource
    private SendAnalysisService sendAnalysisService;

    @GetMapping("/summary")
    @Operation(summary = "获得发送分析概览")
    @PreAuthorize("@ss.hasPermission('marketing:send-analysis:query')")
    public CommonResult<SendAnalysisSummaryRespVO> getSummary(@Valid SendAnalysisReqVO reqVO) {
        return success(sendAnalysisService.getSummary(reqVO));
    }

    @GetMapping("/trend")
    @Operation(summary = "获得发送分析趋势")
    @PreAuthorize("@ss.hasPermission('marketing:send-analysis:query')")
    public CommonResult<List<SendAnalysisTrendRespVO>> getTrend(@Valid SendAnalysisReqVO reqVO) {
        return success(sendAnalysisService.getTrend(reqVO));
    }

    @GetMapping("/channel")
    @Operation(summary = "获得发送渠道分析")
    @PreAuthorize("@ss.hasPermission('marketing:send-analysis:query')")
    public CommonResult<List<SendAnalysisChannelRespVO>> getChannel(@Valid SendAnalysisReqVO reqVO) {
        return success(sendAnalysisService.getChannel(reqVO));
    }

    @GetMapping("/template")
    @Operation(summary = "获得发送模板分析")
    @PreAuthorize("@ss.hasPermission('marketing:send-analysis:query')")
    public CommonResult<List<SendAnalysisTemplateRespVO>> getTemplate(@Valid SendAnalysisReqVO reqVO) {
        return success(sendAnalysisService.getTemplate(reqVO));
    }

    @GetMapping("/fail-reason")
    @Operation(summary = "获得发送失败原因分析")
    @PreAuthorize("@ss.hasPermission('marketing:send-analysis:query')")
    public CommonResult<List<SendAnalysisFailReasonRespVO>> getFailReason(@Valid SendAnalysisReqVO reqVO) {
        return success(sendAnalysisService.getFailReason(reqVO));
    }

}
