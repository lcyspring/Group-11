package com.meession.etm.module.trade.controller.admin.contract;

import com.meession.etm.framework.common.pojo.CommonResult;
import com.meession.etm.module.trade.dal.dataobject.contract.TradeContractDO;
import com.meession.etm.module.trade.service.contract.TradeOrderContractService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.*;

import java.util.List;

import static com.meession.etm.framework.common.pojo.CommonResult.success;

@Tag(name = "管理后台 - 订单合同集成")
@RestController
@RequestMapping("/trade/order/contract")
public class TradeOrderContractController {

    @Resource
    private TradeOrderContractService tradeOrderContractService;

    @PostMapping("/create")
    @Operation(summary = "为订单创建合同")
    public CommonResult<Long> createContractForOrder(@RequestParam Long orderId,
                                                      @RequestParam String contractName,
                                                      @RequestParam(required = false) String attachmentUrls) {
        return success(tradeOrderContractService.createContractForOrder(orderId, contractName, attachmentUrls));
    }

    @PostMapping("/bind")
    @Operation(summary = "绑定合同到订单")
    public CommonResult<Boolean> bindContractToOrder(@RequestParam Long orderId,
                                                      @RequestParam Long contractId) {
        tradeOrderContractService.bindContractToOrder(orderId, contractId);
        return success(true);
    }

    @GetMapping("/order/{orderId}")
    @Operation(summary = "根据订单ID获取合同")
    public CommonResult<TradeContractDO> getContractByOrderId(@PathVariable Long orderId) {
        return success(tradeOrderContractService.getContractByOrderId(orderId));
    }

    @PostMapping("/batch")
    @Operation(summary = "批量获取订单合同")
    public CommonResult<List<TradeContractDO>> getContractsByOrderIds(@RequestBody List<Long> orderIds) {
        return success(tradeOrderContractService.getContractsByOrderIds(orderIds));
    }

    @PutMapping("/status")
    @Operation(summary = "更新合同状态")
    public CommonResult<Boolean> updateContractStatus(@RequestParam Long contractId,
                                                       @RequestParam Integer status) {
        tradeOrderContractService.updateContractStatus(contractId, status);
        return success(true);
    }

    @PostMapping("/sign")
    @Operation(summary = "签署合同")
    public CommonResult<Boolean> signContract(@RequestParam Long contractId,
                                               @RequestParam String signedBy) {
        tradeOrderContractService.signContract(contractId, signedBy);
        return success(true);
    }

}