package com.meession.etm.module.crm.controller.admin.customer;

import com.meession.etm.framework.common.pojo.CommonResult;
import com.meession.etm.framework.common.pojo.PageResult;
import com.meession.etm.module.crm.controller.admin.customer.vo.config.CrmCustomerConfigSaveReqVO;
import com.meession.etm.module.crm.controller.admin.customer.vo.config.CrmCustomerConfigRespVO;
import com.meession.etm.module.crm.service.customer.CrmCustomerConfigService;
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

@Tag(name = "管理后台 - CRM 客户配置")
@RestController
@RequestMapping("/crm/customer-config")
@Validated
public class CrmCustomerConfigController {

    @Resource
    private CrmCustomerConfigService configService;

    @PostMapping("/create")
    @Operation(summary = "创建客户配置")
    @PreAuthorize("@ss.hasPermission('crm:customer-config:create')")
    public CommonResult<Long> createConfig(@Valid @RequestBody CrmCustomerConfigSaveReqVO reqVO) {
        return success(configService.createConfig(reqVO));
    }

    @PutMapping("/update")
    @Operation(summary = "更新客户配置")
    @PreAuthorize("@ss.hasPermission('crm:customer-config:update')")
    public CommonResult<Boolean> updateConfig(@Valid @RequestBody CrmCustomerConfigSaveReqVO reqVO) {
        configService.updateConfig(reqVO);
        return success(true);
    }

    @DeleteMapping("/delete")
    @Operation(summary = "删除客户配置")
    @Parameter(name = "id", description = "编号", required = true, example = "1")
    @PreAuthorize("@ss.hasPermission('crm:customer-config:delete')")
    public CommonResult<Boolean> deleteConfig(@RequestParam("id") Long id) {
        configService.deleteConfig(id);
        return success(true);
    }

    @GetMapping("/get")
    @Operation(summary = "获得客户配置")
    @Parameter(name = "id", description = "编号", required = true, example = "1")
    @PreAuthorize("@ss.hasPermission('crm:customer-config:query')")
    public CommonResult<CrmCustomerConfigRespVO> getConfig(@RequestParam("id") Long id) {
        return success(com.meession.etm.framework.common.util.object.BeanUtils.toBean(configService.getConfig(id), CrmCustomerConfigRespVO.class));
    }

    @GetMapping("/list-by-type")
    @Operation(summary = "根据配置类型获取配置列表")
    @Parameter(name = "configType", description = "配置类型(level/status/source/industry)", required = true, example = "level")
    @PreAuthorize("@ss.hasPermission('crm:customer-config:query')")
    public CommonResult<List<CrmCustomerConfigRespVO>> getConfigListByType(@RequestParam("configType") String configType) {
        return success(configService.getConfigListByTypeVo(configType));
    }

    @GetMapping("/page")
    @Operation(summary = "获得客户配置分页")
    @Parameter(name = "configType", description = "配置类型(level/status/source/industry)", example = "level")
    @PreAuthorize("@ss.hasPermission('crm:customer-config:query')")
    public CommonResult<PageResult<CrmCustomerConfigRespVO>> getConfigPage(@RequestParam(value = "configType", required = false) String configType) {
        return success(configService.getConfigPage(configType));
    }

}