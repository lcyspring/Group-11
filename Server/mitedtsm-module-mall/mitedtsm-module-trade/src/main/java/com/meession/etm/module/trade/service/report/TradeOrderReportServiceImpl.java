package com.meession.etm.module.trade.service.report;

import com.meession.etm.module.trade.controller.admin.report.vo.TradeOrderDailyReportRespVO;
import com.meession.etm.module.trade.controller.admin.report.vo.TradeOrderReportReqVO;
import com.meession.etm.module.trade.controller.admin.report.vo.TradeOrderReportRespVO;
import com.meession.etm.module.trade.dal.mysql.report.TradeOrderReportMapper;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class TradeOrderReportServiceImpl implements TradeOrderReportService {

    @Resource
    private TradeOrderReportMapper tradeOrderReportMapper;

    @Override
    public TradeOrderReportRespVO getOrderReport(TradeOrderReportReqVO reqVO) {
        TradeOrderReportRespVO report = tradeOrderReportMapper.selectReport(reqVO);
        if (report == null) {
            report = new TradeOrderReportRespVO();
        }
        if (report.getTotalCount() != null && report.getTotalCount() > 0) {
            report.setAvgOrderAmount(report.getTotalAmount() / report.getTotalCount());
        }
        return report;
    }

    @Override
    public List<TradeOrderDailyReportRespVO> getDailyReport(TradeOrderReportReqVO reqVO) {
        return tradeOrderReportMapper.selectDailyReport(reqVO);
    }

}