package com.meession.etm.module.marketing.controller.admin.care;

import com.meession.etm.framework.common.pojo.CommonResult;
import com.meession.etm.framework.common.pojo.PageResult;
import com.meession.etm.framework.common.util.object.BeanUtils;
import com.meession.etm.module.marketing.controller.admin.care.vo.CustomerCareConfigPageReqVO;
import com.meession.etm.module.marketing.controller.admin.care.vo.CustomerCareConfigRespVO;
import com.meession.etm.module.marketing.controller.admin.care.vo.CustomerCareConfigSaveReqVO;
import com.meession.etm.module.marketing.dal.dataobject.care.CustomerCareConfigDO;
import com.meession.etm.module.marketing.service.care.CustomerCareService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import static com.meession.etm.framework.common.pojo.CommonResult.success;

@Tag(name = "管理后台 - 客户关怀模板配置")
@RestController
@RequestMapping("/marketing/care-template")
@Validated
public class CustomerCareConfigController {

    @Resource
    private CustomerCareService customerCareService;

    @PostMapping("/create")
    @Operation(summary = "创建客户关怀模板配置")
    @PreAuthorize("@ss.hasPermission('marketing:care-template:create')")
    public CommonResult<Long> createCareConfig(@Valid @RequestBody CustomerCareConfigSaveReqVO createReqVO) {
        return success(customerCareService.createCareConfig(createReqVO));
    }

    @PutMapping("/update")
    @Operation(summary = "更新客户关怀模板配置")
    @PreAuthorize("@ss.hasPermission('marketing:care-template:update')")
    public CommonResult<Boolean> updateCareConfig(@Valid @RequestBody CustomerCareConfigSaveReqVO updateReqVO) {
        customerCareService.updateCareConfig(updateReqVO);
        return success(true);
    }

    @DeleteMapping("/delete")
    @Operation(summary = "删除客户关怀模板配置")
    @Parameter(name = "id", description = "编号", required = true)
    @PreAuthorize("@ss.hasPermission('marketing:care-template:delete')")
    public CommonResult<Boolean> deleteCareConfig(@RequestParam("id") Long id) {
        customerCareService.deleteCareConfig(id);
        return success(true);
    }

    @GetMapping("/get")
    @Operation(summary = "获得客户关怀模板配置")
    @Parameter(name = "id", description = "编号", required = true, example = "1024")
    @PreAuthorize("@ss.hasPermission('marketing:care-template:query')")
    public CommonResult<CustomerCareConfigRespVO> getCareConfig(@RequestParam("id") Long id) {
        CustomerCareConfigDO config = customerCareService.getCareConfig(id);
        return success(BeanUtils.toBean(config, CustomerCareConfigRespVO.class));
    }

    @GetMapping("/page")
    @Operation(summary = "获得客户关怀模板配置分页")
    @PreAuthorize("@ss.hasPermission('marketing:care-template:query')")
    public CommonResult<PageResult<CustomerCareConfigRespVO>> getCareConfigPage(@Valid CustomerCareConfigPageReqVO pageVO) {
        PageResult<CustomerCareConfigDO> pageResult = customerCareService.getCareConfigPage(pageVO);
        return success(BeanUtils.toBean(pageResult, CustomerCareConfigRespVO.class));
    }

}
