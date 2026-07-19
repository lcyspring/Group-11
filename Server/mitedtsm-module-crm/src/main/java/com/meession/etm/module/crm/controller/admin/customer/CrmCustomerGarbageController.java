package com.meession.etm.module.crm.controller.admin.customer;

import cn.hutool.core.collection.CollUtil;
import com.meession.etm.framework.common.pojo.CommonResult;
import com.meession.etm.framework.common.pojo.PageResult;
import com.meession.etm.framework.common.util.collection.MapUtils;
import com.meession.etm.framework.common.util.number.NumberUtils;
import com.meession.etm.framework.common.util.object.BeanUtils;
import com.meession.etm.framework.ip.core.utils.AreaUtils;
import com.meession.etm.module.crm.controller.admin.customer.vo.customer.CrmCustomerRespVO;
import com.meession.etm.module.crm.controller.admin.customer.vo.garbage.CrmCustomerGarbagePageReqVO;
import com.meession.etm.module.crm.controller.admin.customer.vo.garbage.CrmCustomerGarbagePutReqVO;
import com.meession.etm.module.crm.dal.dataobject.customer.CrmCustomerDO;
import com.meession.etm.module.crm.service.customer.CrmCustomerGarbageService;
import com.meession.etm.module.system.api.user.AdminUserApi;
import com.meession.etm.module.system.api.user.dto.AdminUserRespDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static com.meession.etm.framework.common.pojo.CommonResult.success;
import static com.meession.etm.framework.common.util.collection.CollectionUtils.convertListByFlatMap;
import static com.meession.etm.framework.security.core.util.SecurityFrameworkUtils.getLoginUserId;

@Tag(name = "管理后台 - CRM 客户垃圾池")
@RestController
@RequestMapping("/crm/customer-garbage")
@Validated
public class CrmCustomerGarbageController {

    @Resource
    private CrmCustomerGarbageService customerGarbageService;
    @Resource
    private AdminUserApi adminUserApi;

    @GetMapping("/page")
    @Operation(summary = "获得客户垃圾池分页")
    @PreAuthorize("@ss.hasPermission('crm:customer-garbage:query')")
    public CommonResult<PageResult<CrmCustomerRespVO>> getGarbagePage(
            @Valid CrmCustomerGarbagePageReqVO pageReqVO) {
        PageResult<CrmCustomerDO> page = customerGarbageService.getGarbagePage(pageReqVO, getLoginUserId());
        return success(new PageResult<>(buildCustomerList(page.getList()), page.getTotal()));
    }

    @PutMapping("/put")
    @Operation(summary = "将公海客户转入垃圾池")
    @PreAuthorize("@ss.hasPermission('crm:customer-garbage:manage')")
    public CommonResult<Boolean> putCustomerGarbage(@Valid @RequestBody CrmCustomerGarbagePutReqVO reqVO) {
        customerGarbageService.putCustomerGarbage(reqVO, getLoginUserId());
        return success(true);
    }

    @PutMapping("/restore")
    @Operation(summary = "将垃圾客户恢复到公海")
    @Parameter(name = "customerId", description = "客户编号", required = true)
    @PreAuthorize("@ss.hasPermission('crm:customer-garbage:manage')")
    public CommonResult<Boolean> restoreCustomer(@RequestParam("customerId") Long customerId) {
        customerGarbageService.restoreCustomerToPublicPool(customerId, getLoginUserId());
        return success(true);
    }

    @DeleteMapping("/delete-permanently")
    @Operation(summary = "永久删除垃圾客户")
    @Parameter(name = "customerId", description = "客户编号", required = true)
    @PreAuthorize("@ss.hasPermission('crm:customer-garbage:delete')")
    public CommonResult<Boolean> permanentlyDeleteCustomer(@RequestParam("customerId") Long customerId) {
        customerGarbageService.permanentlyDeleteGarbageCustomer(customerId, getLoginUserId());
        return success(true);
    }

    private List<CrmCustomerRespVO> buildCustomerList(List<CrmCustomerDO> customers) {
        if (CollUtil.isEmpty(customers)) {
            return Collections.emptyList();
        }
        Map<Long, AdminUserRespDTO> userMap = adminUserApi.getUserMap(convertListByFlatMap(customers,
                customer -> Stream.of(NumberUtils.parseLong(customer.getCreator()),
                        customer.getPoolPreviousOwnerUserId())));
        return BeanUtils.toBean(customers, CrmCustomerRespVO.class, customer -> {
            customer.setAreaName(AreaUtils.format(customer.getAreaId()));
            MapUtils.findAndThen(userMap, NumberUtils.parseLong(customer.getCreator()),
                    user -> customer.setCreatorName(user.getNickname()));
            MapUtils.findAndThen(userMap, customer.getPoolPreviousOwnerUserId(),
                    user -> customer.setPoolPreviousOwnerUserName(user.getNickname()));
        });
    }
}
