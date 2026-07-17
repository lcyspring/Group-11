package com.meession.etm.module.marketing.service.roi;

import com.meession.etm.module.marketing.controller.admin.roi.vo.RoiAnalysisCampaignRankingRespVO;
import com.meession.etm.module.marketing.controller.admin.roi.vo.RoiAnalysisChannelRespVO;
import com.meession.etm.module.marketing.controller.admin.roi.vo.RoiAnalysisFunnelRespVO;
import com.meession.etm.module.marketing.controller.admin.roi.vo.RoiAnalysisReqVO;
import com.meession.etm.module.marketing.controller.admin.roi.vo.RoiAnalysisSummaryRespVO;
import com.meession.etm.module.marketing.controller.admin.roi.vo.RoiAnalysisTrendRespVO;

import java.util.List;

/**
 * 营销 ROI 分析 Service 接口
 *
 * @author MITEDTSM
 */
public interface RoiAnalysisService {

    RoiAnalysisSummaryRespVO getSummary(RoiAnalysisReqVO reqVO);

    List<RoiAnalysisTrendRespVO> getTrend(RoiAnalysisReqVO reqVO);

    List<RoiAnalysisCampaignRankingRespVO> getCampaignRanking(RoiAnalysisReqVO reqVO);

    List<RoiAnalysisChannelRespVO> getChannel(RoiAnalysisReqVO reqVO);

    RoiAnalysisFunnelRespVO getFunnel(RoiAnalysisReqVO reqVO);

}
