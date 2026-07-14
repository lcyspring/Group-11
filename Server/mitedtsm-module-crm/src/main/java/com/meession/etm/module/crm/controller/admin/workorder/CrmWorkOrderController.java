package com.meession.etm.module.crm.controller.admin.workorder;

import com.meession.etm.framework.common.pojo.CommonResult;
import com.meession.etm.framework.common.pojo.PageResult;
import com.meession.etm.framework.common.util.object.BeanUtils;
import com.meession.etm.framework.security.core.service.SecurityFrameworkService;
import com.meession.etm.module.crm.controller.admin.workorder.vo.*;
import com.meession.etm.module.crm.dal.dataobject.customer.CrmCustomerDO;
import com.meession.etm.module.crm.dal.dataobject.workorder.CrmWorkOrderDO;
import com.meession.etm.module.crm.dal.dataobject.workorder.CrmWorkOrderRecordDO;
import com.meession.etm.module.crm.service.customer.CrmCustomerService;
import com.meession.etm.module.crm.service.workorder.CrmWorkOrderService;
import com.meession.etm.module.system.api.user.AdminUserApi;
import com.meession.etm.module.system.api.user.dto.AdminUserRespDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.meession.etm.framework.common.pojo.CommonResult.success;
import static com.meession.etm.framework.security.core.util.SecurityFrameworkUtils.getLoginUserId;

@Tag(name = "管理后台 - CRM 客服工单")
@RestController
@RequestMapping("/crm/work-order")
@Validated
public class CrmWorkOrderController {

    @Resource private CrmWorkOrderService workOrderService;
    @Resource private CrmCustomerService customerService;
    @Resource private AdminUserApi adminUserApi;
    @Resource private SecurityFrameworkService securityFrameworkService;

    @PostMapping("/create")
    @Operation(summary = "创建客服工单")
    @PreAuthorize("@ss.hasPermission('crm:work-order:create')")
    public CommonResult<Long> create(@Valid @RequestBody CrmWorkOrderSaveReqVO reqVO) {
        return success(workOrderService.createWorkOrder(reqVO, getLoginUserId()));
    }

    @PutMapping("/update")
    @Operation(summary = "修改客服工单")
    @PreAuthorize("@ss.hasPermission('crm:work-order:update')")
    public CommonResult<Boolean> update(@Valid @RequestBody CrmWorkOrderSaveReqVO reqVO) {
        workOrderService.updateWorkOrder(reqVO, getLoginUserId());
        return success(true);
    }

    @DeleteMapping("/delete")
    @Operation(summary = "删除客服工单")
    @Parameter(name = "id", required = true)
    @PreAuthorize("@ss.hasPermission('crm:work-order:delete')")
    public CommonResult<Boolean> delete(@RequestParam Long id) {
        workOrderService.deleteWorkOrder(id, getLoginUserId());
        return success(true);
    }

    @GetMapping("/get")
    @Operation(summary = "获得客服工单")
    @PreAuthorize("@ss.hasPermission('crm:work-order:query')")
    public CommonResult<CrmWorkOrderRespVO> get(@RequestParam Long id) {
        Long userId = getLoginUserId();
        boolean queryAll = securityFrameworkService.hasPermission("crm:work-order:query-all");
        CrmWorkOrderDO order = workOrderService.getWorkOrder(id, userId, queryAll);
        CrmWorkOrderRespVO response = build(Collections.singletonList(order)).get(0);
        response.setRecords(buildRecords(workOrderService.getWorkOrderRecords(id, userId, queryAll)));
        return success(response);
    }

    @GetMapping("/page")
    @Operation(summary = "获得客服工单分页")
    @PreAuthorize("@ss.hasPermission('crm:work-order:query')")
    public CommonResult<PageResult<CrmWorkOrderRespVO>> page(@Valid CrmWorkOrderPageReqVO reqVO) {
        Long userId = getLoginUserId();
        boolean queryAll = securityFrameworkService.hasPermission("crm:work-order:query-all");
        PageResult<CrmWorkOrderDO> result = workOrderService.getWorkOrderPage(reqVO, userId, queryAll);
        return success(new PageResult<>(build(result.getList()), result.getTotal()));
    }

    @PutMapping("/start")
    @Operation(summary = "开始处理客服工单")
    @PreAuthorize("@ss.hasPermission('crm:work-order:process')")
    public CommonResult<Boolean> start(@Valid @RequestBody CrmWorkOrderActionReqVO reqVO) {
        workOrderService.startWorkOrder(reqVO, getLoginUserId());
        return success(true);
    }

    @PutMapping("/return")
    @Operation(summary = "退回客服工单")
    @PreAuthorize("@ss.hasPermission('crm:work-order:process')")
    public CommonResult<Boolean> returnOrder(@Valid @RequestBody CrmWorkOrderReturnReqVO reqVO) {
        workOrderService.returnWorkOrder(reqVO, getLoginUserId());
        return success(true);
    }

    @PutMapping("/resubmit")
    @Operation(summary = "重新提交客服工单")
    @PreAuthorize("@ss.hasPermission('crm:work-order:update')")
    public CommonResult<Boolean> resubmit(@Valid @RequestBody CrmWorkOrderActionReqVO reqVO) {
        workOrderService.resubmitWorkOrder(reqVO, getLoginUserId());
        return success(true);
    }

    @PutMapping("/complete")
    @Operation(summary = "完结客服工单")
    @PreAuthorize("@ss.hasPermission('crm:work-order:process')")
    public CommonResult<Boolean> complete(@Valid @RequestBody CrmWorkOrderCompleteReqVO reqVO) {
        workOrderService.completeWorkOrder(reqVO, getLoginUserId());
        return success(true);
    }

    private List<CrmWorkOrderRespVO> build(List<CrmWorkOrderDO> orders) {
        if (orders == null || orders.isEmpty()) return Collections.emptyList();
        Set<Long> customerIds = orders.stream().map(CrmWorkOrderDO::getCustomerId)
                .filter(Objects::nonNull).collect(Collectors.toSet());
        Map<Long, CrmCustomerDO> customers = customerService.getCustomerMap(customerIds);
        Set<Long> userIds = orders.stream().flatMap(order -> Stream.of(
                        order.getHandlerUserId(), parseUserId(order.getCreator())))
                .filter(Objects::nonNull).collect(Collectors.toSet());
        Map<Long, AdminUserRespDTO> users = adminUserApi.getUserMap(userIds);
        return BeanUtils.toBean(orders, CrmWorkOrderRespVO.class, vo -> {
            Optional.ofNullable(customers.get(vo.getCustomerId())).ifPresent(c -> vo.setCustomerName(c.getName()));
            Optional.ofNullable(users.get(vo.getHandlerUserId())).ifPresent(u -> vo.setHandlerUserName(u.getNickname()));
            Optional.ofNullable(users.get(parseUserId(vo.getCreator()))).ifPresent(u -> vo.setCreatorName(u.getNickname()));
        });
    }

    private List<CrmWorkOrderRecordRespVO> buildRecords(List<CrmWorkOrderRecordDO> records) {
        if (records == null || records.isEmpty()) return Collections.emptyList();
        Set<Long> userIds = records.stream().flatMap(record -> Stream.of(record.getOperatorUserId(), record.getHandlerUserId()))
                .filter(Objects::nonNull).collect(Collectors.toSet());
        Map<Long, AdminUserRespDTO> users = adminUserApi.getUserMap(userIds);
        return BeanUtils.toBean(records, CrmWorkOrderRecordRespVO.class, vo -> {
            Optional.ofNullable(users.get(vo.getOperatorUserId())).ifPresent(u -> vo.setOperatorUserName(u.getNickname()));
            Optional.ofNullable(users.get(vo.getHandlerUserId())).ifPresent(u -> vo.setHandlerUserName(u.getNickname()));
        });
    }

    private static Long parseUserId(String creator) {
        if (creator == null || creator.isBlank()) return null;
        try { return Long.valueOf(creator); } catch (NumberFormatException ignored) { return null; }
    }
}
