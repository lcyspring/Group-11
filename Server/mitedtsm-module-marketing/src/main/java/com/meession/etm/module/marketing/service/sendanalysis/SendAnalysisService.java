package com.meession.etm.module.marketing.service.sendanalysis;

import com.meession.etm.module.marketing.controller.admin.sendanalysis.vo.SendAnalysisChannelRespVO;
import com.meession.etm.module.marketing.controller.admin.sendanalysis.vo.SendAnalysisFailReasonRespVO;
import com.meession.etm.module.marketing.controller.admin.sendanalysis.vo.SendAnalysisReqVO;
import com.meession.etm.module.marketing.controller.admin.sendanalysis.vo.SendAnalysisSummaryRespVO;
import com.meession.etm.module.marketing.controller.admin.sendanalysis.vo.SendAnalysisTemplateRespVO;
import com.meession.etm.module.marketing.controller.admin.sendanalysis.vo.SendAnalysisTrendRespVO;

import java.util.List;

/**
 * 营销发送分析 Service 接口
 *
 * @author MITEDTSM
 */
public interface SendAnalysisService {

    SendAnalysisSummaryRespVO getSummary(SendAnalysisReqVO reqVO);

    List<SendAnalysisTrendRespVO> getTrend(SendAnalysisReqVO reqVO);

    List<SendAnalysisChannelRespVO> getChannel(SendAnalysisReqVO reqVO);

    List<SendAnalysisTemplateRespVO> getTemplate(SendAnalysisReqVO reqVO);

    List<SendAnalysisFailReasonRespVO> getFailReason(SendAnalysisReqVO reqVO);

}
