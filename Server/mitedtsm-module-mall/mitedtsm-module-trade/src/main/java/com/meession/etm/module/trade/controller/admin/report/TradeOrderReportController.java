package com.meession.etm.module.trade.controller.admin.report;

import com.meession.etm.module.trade.controller.admin.report.vo.TradeOrderDailyReportRespVO;
import com.meession.etm.module.trade.controller.admin.report.vo.TradeOrderReportReqVO;
import com.meession.etm.module.trade.controller.admin.report.vo.TradeOrderReportRespVO;
import com.meession.etm.module.trade.service.report.TradeOrderReportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "管理后台 - 订单报表")
@RestController
@RequestMapping("/trade/order/report")
public class TradeOrderReportController {

    @Resource
    private TradeOrderReportService tradeOrderReportService;

    @GetMapping("/summary")
    @Operation(summary = "获取订单汇总报表")
    public TradeOrderReportRespVO getOrderReport(@Valid TradeOrderReportReqVO reqVO) {
        return tradeOrderReportService.getOrderReport(reqVO);
    }

    @GetMapping("/daily")
    @Operation(summary = "获取订单日报表")
    public List<TradeOrderDailyReportRespVO> getDailyReport(@Valid TradeOrderReportReqVO reqVO) {
        return tradeOrderReportService.getDailyReport(reqVO);
    }

}