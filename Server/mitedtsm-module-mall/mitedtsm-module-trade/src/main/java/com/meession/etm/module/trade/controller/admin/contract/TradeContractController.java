package com.meession.etm.module.trade.controller.admin.contract;

import com.meession.etm.framework.common.pojo.CommonResult;
import com.meession.etm.framework.common.pojo.PageResult;
import com.meession.etm.framework.common.util.object.BeanUtils;
import com.meession.etm.module.trade.controller.admin.contract.vo.TradeContractPageReqVO;
import com.meession.etm.module.trade.controller.admin.contract.vo.TradeContractRespVO;
import com.meession.etm.module.trade.controller.admin.contract.vo.TradeContractSaveReqVO;
import com.meession.etm.module.trade.dal.dataobject.contract.TradeContractDO;
import com.meession.etm.module.trade.service.TradeContractService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

import static com.meession.etm.framework.common.pojo.CommonResult.success;

@Tag(name = "管理后台 - 合同")
@RestController
@RequestMapping("/trade/contract")
public class TradeContractController {

    @Resource
    private TradeContractService tradeContractService;

    @PostMapping("/create")
    @Operation(summary = "创建合同")
    @PreAuthorize("@ss.hasPermission('trade:contract:create')")
    public CommonResult<Long> createContract(@Valid @RequestBody TradeContractSaveReqVO createReqVO) {
        Long id = tradeContractService.createContract(createReqVO);
        return success(id);
    }

    @PutMapping("/update")
    @Operation(summary = "更新合同")
    @PreAuthorize("@ss.hasPermission('trade:contract:update')")
    public CommonResult<Void> updateContract(@Valid @RequestBody TradeContractSaveReqVO updateReqVO) {
        tradeContractService.updateContract(updateReqVO);
        return success();
    }

    @DeleteMapping("/delete")
    @Operation(summary = "删除合同")
    @PreAuthorize("@ss.hasPermission('trade:contract:delete')")
    public CommonResult<Void> deleteContract(@Parameter(name = "id", description = "合同编号", required = true) @RequestParam("id") Long id) {
        tradeContractService.deleteContract(id);
        return success();
    }

    @GetMapping("/get-detail")
    @Operation(summary = "获取合同详情")
    @PreAuthorize("@ss.hasPermission('trade:contract:query')")
    public CommonResult<TradeContractRespVO> getContractDetail(@Parameter(name = "id", description = "合同编号", required = true) @RequestParam("id") Long id) {
        TradeContractDO contract = tradeContractService.getContract(id);
        return success(BeanUtils.toBean(contract, TradeContractRespVO.class));
    }

    @GetMapping("/page")
    @Operation(summary = "获取合同分页")
    @PreAuthorize("@ss.hasPermission('trade:contract:query')")
    public CommonResult<PageResult<TradeContractRespVO>> getContractPage(@Valid TradeContractPageReqVO pageReqVO) {
        PageResult<TradeContractDO> pageResult = tradeContractService.getContractPage(pageReqVO);
        return success(BeanUtils.toBean(pageResult, TradeContractRespVO.class));
    }

    @GetMapping("/list-by-order")
    @Operation(summary = "根据订单获取合同列表")
    @PreAuthorize("@ss.hasPermission('trade:contract:query')")
    public CommonResult<List<TradeContractRespVO>> getContractsByOrderId(@Parameter(name = "orderId", description = "订单编号", required = true) @RequestParam("orderId") Long orderId) {
        List<TradeContractDO> contracts = tradeContractService.getContractsByOrderId(orderId);
        return success(BeanUtils.toBean(contracts, TradeContractRespVO.class));
    }

    @GetMapping("/list-by-customer")
    @Operation(summary = "根据客户获取合同列表")
    @PreAuthorize("@ss.hasPermission('trade:contract:query')")
    public CommonResult<List<TradeContractRespVO>> getContractsByCustomerId(@Parameter(name = "customerId", description = "客户编号", required = true) @RequestParam("customerId") Long customerId) {
        List<TradeContractDO> contracts = tradeContractService.getContractsByCustomerId(customerId);
        return success(BeanUtils.toBean(contracts, TradeContractRespVO.class));
    }

    @PutMapping("/update-status")
    @Operation(summary = "更新合同状态")
    @PreAuthorize("@ss.hasPermission('trade:contract:update')")
    public CommonResult<Void> updateContractStatus(@Parameter(name = "id", description = "合同编号", required = true) @RequestParam("id") Long id,
                                                    @Parameter(name = "status", description = "状态", required = true) @RequestParam("status") Integer status) {
        tradeContractService.updateContractStatus(id, status);
        return success();
    }

}