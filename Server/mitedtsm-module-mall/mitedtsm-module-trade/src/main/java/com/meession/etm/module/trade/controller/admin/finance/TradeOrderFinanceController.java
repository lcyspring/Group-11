package com.meession.etm.module.trade.controller.admin.finance;

import com.meession.etm.framework.common.pojo.CommonResult;
import com.meession.etm.module.trade.controller.admin.finance.vo.TradeOrderPaymentReqVO;
import com.meession.etm.module.trade.controller.admin.finance.vo.TradeOrderPaymentRespVO;
import com.meession.etm.module.trade.controller.admin.finance.vo.TradeOrderRefundReqVO;
import com.meession.etm.module.trade.service.finance.TradeOrderFinanceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

import static com.meession.etm.framework.common.pojo.CommonResult.success;

@Tag(name = "管理后台 - 订单财务集成")
@RestController
@RequestMapping("/trade/order/finance")
public class TradeOrderFinanceController {

    @Resource
    private TradeOrderFinanceService tradeOrderFinanceService;

    @PostMapping("/payment")
    @Operation(summary = "处理订单支付")
    public CommonResult<Long> processPayment(@RequestHeader("userId") Long userId,
                                              @Valid @RequestBody TradeOrderPaymentReqVO reqVO) {
        return success(tradeOrderFinanceService.processPayment(userId, reqVO));
    }

    @PostMapping("/refund")
    @Operation(summary = "处理订单退款")
    public CommonResult<Long> processRefund(@RequestHeader("userId") Long userId,
                                             @Valid @RequestBody TradeOrderRefundReqVO reqVO) {
        return success(tradeOrderFinanceService.processRefund(userId, reqVO));
    }

    @GetMapping("/payment/{paymentId}")
    @Operation(summary = "获取支付记录")
    public CommonResult<TradeOrderPaymentRespVO> getPaymentRecord(@PathVariable Long paymentId) {
        return success(tradeOrderFinanceService.getPaymentRecord(paymentId));
    }

    @GetMapping("/payment/order/{orderId}")
    @Operation(summary = "获取订单支付记录列表")
    public CommonResult<List<TradeOrderPaymentRespVO>> getPaymentRecordsByOrderId(@PathVariable Long orderId) {
        return success(tradeOrderFinanceService.getPaymentRecordsByOrderId(orderId));
    }

    @PostMapping("/sync/{orderId}")
    @Operation(summary = "同步订单支付状态")
    public CommonResult<Boolean> syncPaymentStatus(@PathVariable Long orderId) {
        tradeOrderFinanceService.syncPaymentStatus(orderId);
        return success(true);
    }

}