package com.meession.etm.module.trade.controller.admin.analysis;

import com.meession.etm.module.trade.controller.admin.analysis.vo.TradeOrderAnalysisReqVO;
import com.meession.etm.module.trade.controller.admin.analysis.vo.TradeOrderAnalysisRespVO;
import com.meession.etm.module.trade.service.analysis.TradeOrderAnalysisService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "管理后台 - 订单数据分析")
@RestController
@RequestMapping("/trade/order/analysis")
public class TradeOrderAnalysisController {

    @Resource
    private TradeOrderAnalysisService tradeOrderAnalysisService;

    @GetMapping("/summary")
    @Operation(summary = "获取订单数据分析")
    public TradeOrderAnalysisRespVO getOrderAnalysis(@Valid TradeOrderAnalysisReqVO reqVO) {
        return tradeOrderAnalysisService.getOrderAnalysis(reqVO);
    }

}