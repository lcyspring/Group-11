package com.meession.etm.module.crm.controller.admin.fulfillment;

import com.meession.etm.framework.common.pojo.CommonResult;
import com.meession.etm.module.crm.controller.admin.fulfillment.vo.CrmContractFulfillmentRespVO;
import com.meession.etm.module.crm.service.fulfillment.CrmContractFulfillmentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import static com.meession.etm.framework.common.pojo.CommonResult.success;

@Tag(name = "管理后台 - CRM 合同 ERP 履约")
@RestController
@RequestMapping("/crm/contract-fulfillment")
@Validated
public class CrmContractFulfillmentController {

    @Resource
    private CrmContractFulfillmentService fulfillmentService;

    @GetMapping("/get")
    @Operation(summary = "获得合同 ERP 履约状态和映射准备情况")
    @PreAuthorize("@ss.hasPermission('crm:erp-fulfillment:query')")
    public CommonResult<CrmContractFulfillmentRespVO> get(@RequestParam Long contractId) {
        return success(fulfillmentService.getFulfillment(contractId));
    }

    @PostMapping("/create-or-retry")
    @Operation(summary = "显式创建或幂等重试 ERP 履约订单")
    @PreAuthorize("@ss.hasPermission('crm:erp-fulfillment:create')")
    public CommonResult<CrmContractFulfillmentRespVO> createOrRetry(@RequestParam Long contractId) {
        return success(fulfillmentService.createOrRetry(contractId));
    }

    @PostMapping("/refresh")
    @Operation(summary = "显式刷新 ERP 履约状态")
    @PreAuthorize("@ss.hasPermission('crm:erp-fulfillment:refresh')")
    public CommonResult<CrmContractFulfillmentRespVO> refresh(@RequestParam Long contractId) {
        return success(fulfillmentService.refresh(contractId));
    }
}
