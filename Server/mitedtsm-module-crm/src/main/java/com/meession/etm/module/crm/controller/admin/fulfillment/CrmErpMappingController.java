package com.meession.etm.module.crm.controller.admin.fulfillment;

import com.meession.etm.framework.common.pojo.CommonResult;
import com.meession.etm.framework.common.pojo.PageParam;
import com.meession.etm.framework.common.pojo.PageResult;
import com.meession.etm.module.crm.controller.admin.fulfillment.vo.CrmErpCustomerMappingRespVO;
import com.meession.etm.module.crm.controller.admin.fulfillment.vo.CrmErpMappingSaveReqVO;
import com.meession.etm.module.crm.controller.admin.fulfillment.vo.CrmErpProductMappingRespVO;
import com.meession.etm.module.crm.service.fulfillment.CrmErpMappingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import static com.meession.etm.framework.common.pojo.CommonResult.success;

@Tag(name = "管理后台 - CRM ERP 主数据映射")
@RestController
@RequestMapping("/crm/erp-mapping")
@Validated
public class CrmErpMappingController {

    @Resource
    private CrmErpMappingService mappingService;

    @GetMapping("/customer/page")
    @Operation(summary = "客户映射分页")
    @PreAuthorize("@ss.hasPermission('crm:erp-mapping:query')")
    public CommonResult<PageResult<CrmErpCustomerMappingRespVO>> getCustomerPage(@Valid PageParam pageParam) {
        return success(mappingService.getCustomerMappingPage(pageParam));
    }

    @GetMapping("/product/page")
    @Operation(summary = "产品映射分页")
    @PreAuthorize("@ss.hasPermission('crm:erp-mapping:query')")
    public CommonResult<PageResult<CrmErpProductMappingRespVO>> getProductPage(@Valid PageParam pageParam) {
        return success(mappingService.getProductMappingPage(pageParam));
    }

    @PostMapping("/customer/save")
    @Operation(summary = "新增或更新客户映射")
    @PreAuthorize("@ss.hasPermission('crm:erp-mapping:update')")
    public CommonResult<Long> saveCustomer(@Valid @RequestBody CrmErpMappingSaveReqVO request) {
        return success(mappingService.saveCustomerMapping(request));
    }

    @PostMapping("/product/save")
    @Operation(summary = "新增或更新产品映射")
    @PreAuthorize("@ss.hasPermission('crm:erp-mapping:update')")
    public CommonResult<Long> saveProduct(@Valid @RequestBody CrmErpMappingSaveReqVO request) {
        return success(mappingService.saveProductMapping(request));
    }

    @DeleteMapping("/customer/delete")
    @Operation(summary = "删除客户映射")
    @PreAuthorize("@ss.hasPermission('crm:erp-mapping:delete')")
    public CommonResult<Boolean> deleteCustomer(@RequestParam Long id) {
        mappingService.deleteCustomerMapping(id);
        return success(true);
    }

    @DeleteMapping("/product/delete")
    @Operation(summary = "删除产品映射")
    @PreAuthorize("@ss.hasPermission('crm:erp-mapping:delete')")
    public CommonResult<Boolean> deleteProduct(@RequestParam Long id) {
        mappingService.deleteProductMapping(id);
        return success(true);
    }
}
