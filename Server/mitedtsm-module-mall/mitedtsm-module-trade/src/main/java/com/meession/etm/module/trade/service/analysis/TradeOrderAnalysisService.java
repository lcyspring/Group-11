package com.meession.etm.module.trade.service.analysis;

import com.meession.etm.module.trade.controller.admin.analysis.vo.TradeOrderAnalysisReqVO;
import com.meession.etm.module.trade.controller.admin.analysis.vo.TradeOrderAnalysisRespVO;

public interface TradeOrderAnalysisService {

    TradeOrderAnalysisRespVO getOrderAnalysis(TradeOrderAnalysisReqVO reqVO);

}