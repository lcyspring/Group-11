package com.meession.etm.module.crm.controller.admin.customer;

import com.meession.etm.framework.common.pojo.CommonResult;
import com.meession.etm.framework.common.pojo.PageResult;
import com.meession.etm.framework.common.util.object.BeanUtils;
import com.meession.etm.module.crm.controller.admin.customer.vo.poolrule.CrmCustomerPoolRulePageReqVO;
import com.meession.etm.module.crm.controller.admin.customer.vo.poolrule.CrmCustomerPoolRuleRespVO;
import com.meession.etm.module.crm.controller.admin.customer.vo.poolrule.CrmCustomerPoolRuleSaveReqVO;
import com.meession.etm.module.crm.dal.dataobject.customer.CrmCustomerPoolRuleDO;
import com.meession.etm.module.crm.service.customer.CrmCustomerPoolRuleService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

import static com.meession.etm.framework.common.pojo.CommonResult.success;

@Tag(name = "管理后台 - CRM 公海规则")
@RestController
@RequestMapping("/crm/customer-pool-rule")
@Validated
public class CrmCustomerPoolRuleController {

    @Resource
    private CrmCustomerPoolRuleService poolRuleService;

    @PostMapping("/create")
    @Operation(summary = "创建公海规则")
    @PreAuthorize("@ss.hasPermission('crm:customer-pool-rule:create')")
    public CommonResult<Long> createPoolRule(@Valid @RequestBody CrmCustomerPoolRuleSaveReqVO saveReqVO) {
        return success(poolRuleService.createPoolRule(saveReqVO));
    }

    @PutMapping("/update")
    @Operation(summary = "更新公海规则")
    @PreAuthorize("@ss.hasPermission('crm:customer-pool-rule:update')")
    public CommonResult<Boolean> updatePoolRule(@Valid @RequestBody CrmCustomerPoolRuleSaveReqVO saveReqVO) {
        poolRuleService.updatePoolRule(saveReqVO);
        return success(true);
    }

    @DeleteMapping("/delete")
    @Operation(summary = "删除公海规则")
    @Parameter(name = "id", description = "规则编号", required = true)
    @PreAuthorize("@ss.hasPermission('crm:customer-pool-rule:delete')")
    public CommonResult<Boolean> deletePoolRule(@RequestParam("id") Long id) {
        poolRuleService.deletePoolRule(id);
        return success(true);
    }

    @GetMapping("/get")
    @Operation(summary = "获取公海规则")
    @Parameter(name = "id", description = "规则编号", required = true)
    @PreAuthorize("@ss.hasPermission('crm:customer-pool-rule:query')")
    public CommonResult<CrmCustomerPoolRuleRespVO> getPoolRule(@RequestParam("id") Long id) {
        CrmCustomerPoolRuleDO rule = poolRuleService.getPoolRule(id);
        return success(CrmCustomerPoolRuleRespVO.of(rule));
    }

    @GetMapping("/list")
    @Operation(summary = "获取公海规则列表")
    @PreAuthorize("@ss.hasPermission('crm:customer-pool-rule:query')")
    public CommonResult<List<CrmCustomerPoolRuleRespVO>> getPoolRuleList() {
        List<CrmCustomerPoolRuleDO> list = poolRuleService.getPoolRuleList();
        return success(BeanUtils.toBean(list, CrmCustomerPoolRuleRespVO.class));
    }

    @GetMapping("/page")
    @Operation(summary = "获取公海规则分页")
    @PreAuthorize("@ss.hasPermission('crm:customer-pool-rule:query')")
    public CommonResult<PageResult<CrmCustomerPoolRuleRespVO>> getPoolRulePage(@Valid CrmCustomerPoolRulePageReqVO pageReqVO) {
        PageResult<CrmCustomerPoolRuleDO> pageResult = poolRuleService.getPoolRulePage(pageReqVO);
        return success(BeanUtils.toBean(pageResult, CrmCustomerPoolRuleRespVO.class));
    }

    @PutMapping("/enable")
    @Operation(summary = "启用/禁用公海规则")
    @Parameter(name = "id", description = "规则编号", required = true)
    @Parameter(name = "enabled", description = "是否启用", required = true)
    @PreAuthorize("@ss.hasPermission('crm:customer-pool-rule:update')")
    public CommonResult<Boolean> enablePoolRule(@RequestParam("id") Long id, @RequestParam("enabled") Boolean enabled) {
        poolRuleService.enablePoolRule(id, enabled);
        return success(true);
    }

    @PostMapping("/execute")
    @Operation(summary = "执行公海规则")
    @Parameter(name = "id", description = "规则编号", required = true)
    @PreAuthorize("@ss.hasPermission('crm:customer-pool-rule:update')")
    public CommonResult<Boolean> executePoolRule(@RequestParam("id") Long id) {
        poolRuleService.executeRule(id);
        return success(true);
    }

}
