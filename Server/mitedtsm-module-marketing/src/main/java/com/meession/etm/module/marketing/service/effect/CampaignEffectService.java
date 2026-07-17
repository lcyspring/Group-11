package com.meession.etm.module.marketing.service.effect;

import com.meession.etm.module.marketing.controller.admin.effect.vo.CampaignEffectChannelRespVO;
import com.meession.etm.module.marketing.controller.admin.effect.vo.CampaignEffectFunnelRespVO;
import com.meession.etm.module.marketing.controller.admin.effect.vo.CampaignEffectRankingRespVO;
import com.meession.etm.module.marketing.controller.admin.effect.vo.CampaignEffectReqVO;
import com.meession.etm.module.marketing.controller.admin.effect.vo.CampaignEffectSummaryRespVO;
import com.meession.etm.module.marketing.controller.admin.effect.vo.CampaignEffectTrendRespVO;

import java.util.List;

/**
 * 营销活动效果分析 Service 接口
 *
 * @author MITEDTSM
 */
public interface CampaignEffectService {

    CampaignEffectSummaryRespVO getSummary(CampaignEffectReqVO reqVO);

    List<CampaignEffectTrendRespVO> getTrend(CampaignEffectReqVO reqVO);

    List<CampaignEffectRankingRespVO> getRanking(CampaignEffectReqVO reqVO);

    CampaignEffectFunnelRespVO getFunnel(CampaignEffectReqVO reqVO);

    List<CampaignEffectChannelRespVO> getChannel(CampaignEffectReqVO reqVO);

}
