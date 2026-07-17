package com.meession.etm.module.trade.service.report;

import com.meession.etm.module.trade.controller.admin.report.vo.TradeOrderDailyReportRespVO;
import com.meession.etm.module.trade.controller.admin.report.vo.TradeOrderReportReqVO;
import com.meession.etm.module.trade.controller.admin.report.vo.TradeOrderReportRespVO;

import java.util.List;

public interface TradeOrderReportService {

    TradeOrderReportRespVO getOrderReport(TradeOrderReportReqVO reqVO);

    List<TradeOrderDailyReportRespVO> getDailyReport(TradeOrderReportReqVO reqVO);

}