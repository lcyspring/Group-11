package com.meession.etm.module.crm.controller.admin.marketing;

import com.meession.etm.framework.common.pojo.CommonResult;
import com.meession.etm.framework.common.pojo.PageResult;
import com.meession.etm.framework.common.util.object.BeanUtils;
import com.meession.etm.module.crm.controller.admin.marketing.vo.*;
import com.meession.etm.module.crm.dal.dataobject.marketing.CrmCustomerCarePlanDO;
import com.meession.etm.module.crm.service.marketing.CrmCustomerCareService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import static com.meession.etm.framework.common.pojo.CommonResult.success;
import static com.meession.etm.framework.security.core.util.SecurityFrameworkUtils.getLoginUserId;

@Tag(name = "管理后台 - CRM 客户关怀")
@RestController
@RequestMapping("/crm/marketing/care")
@Validated
public class CrmCustomerCareController {
    @Resource private CrmCustomerCareService service;

    @PostMapping("/plan/save")
    @Operation(summary = "保存客户关怀计划")
    @PreAuthorize("@ss.hasPermission('crm:customer-care:update')")
    public CommonResult<Long> savePlan(@Valid @RequestBody CrmCustomerCarePlanSaveReqVO request) { return success(service.savePlan(request)); }

    @GetMapping("/plan/get")
    @Operation(summary = "获得客户关怀计划详情")
    @PreAuthorize("@ss.hasPermission('crm:customer-care:query')")
    public CommonResult<CrmCustomerCarePlanRespVO> getPlan(@RequestParam Long id) {
        return success(BeanUtils.toBean(service.getPlan(id), CrmCustomerCarePlanRespVO.class));
    }

    @PutMapping("/plan/status")
    @Operation(summary = "启用或停用客户关怀计划")
    @PreAuthorize("@ss.hasPermission('crm:customer-care:update')")
    public CommonResult<Boolean> updatePlanStatus(@Valid @RequestBody CrmCustomerCarePlanStatusReqVO request) {
        service.updatePlanStatus(request);
        return success(true);
    }

    @DeleteMapping("/plan/delete")
    @Operation(summary = "删除已停用的客户关怀计划")
    @PreAuthorize("@ss.hasPermission('crm:customer-care:delete')")
    public CommonResult<Boolean> deletePlan(@RequestParam Long id) {
        service.deletePlan(id);
        return success(true);
    }

    @GetMapping("/plan/page")
    @Operation(summary = "获得客户关怀计划分页")
    @PreAuthorize("@ss.hasPermission('crm:customer-care:query')")
    public CommonResult<PageResult<CrmCustomerCarePlanRespVO>> getPlanPage(@Valid CrmCustomerCarePlanPageReqVO request) {
        PageResult<CrmCustomerCarePlanDO> page = service.getPlanPage(request);
        return success(new PageResult<>(BeanUtils.toBean(page.getList(), CrmCustomerCarePlanRespVO.class), page.getTotal()));
    }

    @GetMapping("/record/page")
    @Operation(summary = "获得客户关怀记录分页")
    @PreAuthorize("@ss.hasPermission('crm:customer-care:query')")
    public CommonResult<PageResult<CrmCustomerCareRecordRespVO>> getRecordPage(@Valid CrmCustomerCareRecordPageReqVO request) {
        return success(service.getRecordPage(request, getLoginUserId()));
    }

    @GetMapping("/birthday/page")
    @Operation(summary = "获得当前用户数据范围内的近期客户联系人生日")
    @PreAuthorize("@ss.hasPermission('crm:customer-care:query')")
    public CommonResult<PageResult<CrmCustomerBirthdayRespVO>> getBirthdayPage(
            @Valid CrmCustomerBirthdayPageReqVO request) {
        return success(service.getBirthdayPage(request, getLoginUserId()));
    }
}
