package com.meession.etm.module.marketing.controller.admin.effect;

import com.meession.etm.framework.common.pojo.CommonResult;
import com.meession.etm.module.marketing.controller.admin.effect.vo.CampaignEffectChannelRespVO;
import com.meession.etm.module.marketing.controller.admin.effect.vo.CampaignEffectFunnelRespVO;
import com.meession.etm.module.marketing.controller.admin.effect.vo.CampaignEffectRankingRespVO;
import com.meession.etm.module.marketing.controller.admin.effect.vo.CampaignEffectReqVO;
import com.meession.etm.module.marketing.controller.admin.effect.vo.CampaignEffectSummaryRespVO;
import com.meession.etm.module.marketing.controller.admin.effect.vo.CampaignEffectTrendRespVO;
import com.meession.etm.module.marketing.service.effect.CampaignEffectService;
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

@Tag(name = "管理后台 - 营销活动效果分析")
@RestController
@RequestMapping("/marketing/campaign-effect")
@Validated
public class CampaignEffectController {

    @Resource
    private CampaignEffectService campaignEffectService;

    @GetMapping("/summary")
    @Operation(summary = "获得营销活动效果概览")
    @PreAuthorize("@ss.hasPermission('marketing:campaign-effect:query')")
    public CommonResult<CampaignEffectSummaryRespVO> getSummary(@Valid CampaignEffectReqVO reqVO) {
        return success(campaignEffectService.getSummary(reqVO));
    }

    @GetMapping("/trend")
    @Operation(summary = "获得营销活动效果趋势")
    @PreAuthorize("@ss.hasPermission('marketing:campaign-effect:query')")
    public CommonResult<List<CampaignEffectTrendRespVO>> getTrend(@Valid CampaignEffectReqVO reqVO) {
        return success(campaignEffectService.getTrend(reqVO));
    }

    @GetMapping("/ranking")
    @Operation(summary = "获得营销活动效果排行")
    @PreAuthorize("@ss.hasPermission('marketing:campaign-effect:query')")
    public CommonResult<List<CampaignEffectRankingRespVO>> getRanking(@Valid CampaignEffectReqVO reqVO) {
        return success(campaignEffectService.getRanking(reqVO));
    }

    @GetMapping("/funnel")
    @Operation(summary = "获得营销活动转化漏斗")
    @PreAuthorize("@ss.hasPermission('marketing:campaign-effect:query')")
    public CommonResult<CampaignEffectFunnelRespVO> getFunnel(@Valid CampaignEffectReqVO reqVO) {
        return success(campaignEffectService.getFunnel(reqVO));
    }

    @GetMapping("/channel")
    @Operation(summary = "获得营销活动渠道效果")
    @PreAuthorize("@ss.hasPermission('marketing:campaign-effect:query')")
    public CommonResult<List<CampaignEffectChannelRespVO>> getChannel(@Valid CampaignEffectReqVO reqVO) {
        return success(campaignEffectService.getChannel(reqVO));
    }

}
