package com.meession.etm.module.crm.controller.admin.marketing;

import com.meession.etm.framework.common.pojo.CommonResult;
import com.meession.etm.framework.common.pojo.PageResult;
import com.meession.etm.framework.common.util.object.BeanUtils;
import com.meession.etm.module.crm.controller.admin.marketing.vo.*;
import com.meession.etm.module.crm.dal.dataobject.marketing.CrmCustomerCarePlanDO;
import com.meession.etm.module.crm.dal.dataobject.marketing.CrmCustomerCareRecordDO;
import com.meession.etm.module.crm.service.marketing.CrmCustomerCareService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import static com.meession.etm.framework.common.pojo.CommonResult.success;

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
        PageResult<CrmCustomerCareRecordDO> page = service.getRecordPage(request);
        return success(new PageResult<>(BeanUtils.toBean(page.getList(), CrmCustomerCareRecordRespVO.class), page.getTotal()));
    }
}
