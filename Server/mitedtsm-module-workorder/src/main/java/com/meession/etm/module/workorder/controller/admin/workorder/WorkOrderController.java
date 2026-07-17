package com.meession.etm.module.workorder.controller.admin.workorder;

import cn.hutool.core.collection.CollUtil;
import com.meession.etm.framework.common.pojo.CommonResult;
import com.meession.etm.framework.common.pojo.PageResult;
import com.meession.etm.framework.common.util.object.BeanUtils;
import com.meession.etm.framework.excel.core.util.ExcelUtils;
import com.meession.etm.module.system.api.user.AdminUserApi;
import com.meession.etm.module.system.api.user.dto.AdminUserRespDTO;
import com.meession.etm.module.workorder.controller.admin.workorder.vo.workorder.*;
import com.meession.etm.module.workorder.dal.dataobject.WorkOrderDO;
import com.meession.etm.module.workorder.dal.dataobject.WorkOrderTypeDO;
import com.meession.etm.module.workorder.enums.WorkOrderPriorityEnum;
import com.meession.etm.module.workorder.enums.WorkOrderStatusEnum;
import com.meession.etm.module.workorder.service.WorkOrderService;
import com.meession.etm.module.workorder.service.WorkOrderTypeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static com.meession.etm.framework.common.pojo.CommonResult.success;
import static com.meession.etm.framework.common.pojo.PageParam.PAGE_SIZE_NONE;
import static com.meession.etm.framework.common.util.collection.CollectionUtils.*;
import static com.meession.etm.framework.security.core.util.SecurityFrameworkUtils.getLoginUserId;

@Tag(name = "管理后台 - 工单管理")
@RestController
@RequestMapping("/workorder/work-order")
@Validated
public class WorkOrderController {

    @Resource
    private WorkOrderService workOrderService;
    @Resource
    private WorkOrderTypeService workOrderTypeService;

    @Resource
    private AdminUserApi adminUserApi;

    @PostMapping("/create")
    @Operation(summary = "创建工单")
    @PreAuthorize("@ss.hasPermission('workorder:work-order:create')")
    public CommonResult<Long> createWorkOrder(@Valid @RequestBody WorkOrderSaveReqVO createReqVO) {
        return success(workOrderService.createWorkOrder(createReqVO, getLoginUserId()));
    }

    @PutMapping("/update")
    @Operation(summary = "更新工单")
    @PreAuthorize("@ss.hasPermission('workorder:work-order:update')")
    public CommonResult<Boolean> updateWorkOrder(@Valid @RequestBody WorkOrderSaveReqVO updateReqVO) {
        workOrderService.updateWorkOrder(updateReqVO);
        return success(true);
    }

    @PutMapping("/update-status")
    @Operation(summary = "更新工单状态")
    @PreAuthorize("@ss.hasPermission('workorder:work-order:update')")
    public CommonResult<Boolean> updateWorkOrderStatus(@Valid @RequestBody WorkOrderUpdateStatusReqVO updateStatusReqVO) {
        workOrderService.updateWorkOrderStatus(updateStatusReqVO);
        return success(true);
    }

    @PutMapping("/update-priority")
    @Operation(summary = "更新工单优先级")
    @PreAuthorize("@ss.hasPermission('workorder:work-order:update')")
    public CommonResult<Boolean> updateWorkOrderPriority(@Valid @RequestBody WorkOrderUpdatePriorityReqVO updatePriorityReqVO) {
        workOrderService.updateWorkOrderPriority(updatePriorityReqVO);
        return success(true);
    }

    @PutMapping("/assign")
    @Operation(summary = "分配工单")
    @PreAuthorize("@ss.hasPermission('workorder:work-order:update')")
    public CommonResult<Boolean> assignWorkOrder(@Valid @RequestBody WorkOrderAssignReqVO assignReqVO) {
        workOrderService.assignWorkOrder(assignReqVO);
        return success(true);
    }

    @PutMapping("/process")
    @Operation(summary = "处理工单（开始处理）")
    @PreAuthorize("@ss.hasPermission('workorder:work-order:update')")
    public CommonResult<Boolean> processWorkOrder(@Valid @RequestBody WorkOrderProcessReqVO processReqVO) {
        workOrderService.processWorkOrder(processReqVO, getLoginUserId());
        return success(true);
    }

    @PutMapping("/complete")
    @Operation(summary = "完结工单")
    @PreAuthorize("@ss.hasPermission('workorder:work-order:update')")
    public CommonResult<Boolean> completeWorkOrder(@Valid @RequestBody WorkOrderCompleteReqVO completeReqVO) {
        workOrderService.completeWorkOrder(completeReqVO);
        return success(true);
    }

    @PutMapping("/return")
    @Operation(summary = "退回工单")
    @PreAuthorize("@ss.hasPermission('workorder:work-order:update')")
    public CommonResult<Boolean> returnWorkOrder(@Valid @RequestBody WorkOrderReturnReqVO returnReqVO) {
        workOrderService.returnWorkOrder(returnReqVO);
        return success(true);
    }

    @GetMapping("/statistics")
    @Operation(summary = "获取工单统计")
    @PreAuthorize("@ss.hasPermission('workorder:work-order:query')")
    public CommonResult<WorkOrderStatisticsRespVO> getWorkOrderStatistics() {
        return success(workOrderService.getWorkOrderStatistics());
    }

    @GetMapping("/efficiency-analysis")
    @Operation(summary = "获取工单效率分析")
    @PreAuthorize("@ss.hasPermission('workorder:work-order:query')")
    public CommonResult<WorkOrderEfficiencyRespVO> getWorkOrderEfficiencyAnalysis() {
        return success(workOrderService.getWorkOrderEfficiencyAnalysis());
    }

    @GetMapping("/trend-analysis")
    @Operation(summary = "获取工单趋势分析")
    @PreAuthorize("@ss.hasPermission('workorder:work-order:query')")
    public CommonResult<WorkOrderTrendRespVO> getWorkOrderTrendAnalysis(WorkOrderTrendReqVO reqVO) {
        return success(workOrderService.getWorkOrderTrendAnalysis(reqVO));
    }

    @DeleteMapping("/delete")
    @Operation(summary = "删除工单")
    @Parameter(name = "id", description = "工单编号", required = true)
    @PreAuthorize("@ss.hasPermission('workorder:work-order:delete')")
    public CommonResult<Boolean> deleteWorkOrder(@RequestParam("id") Long id) {
        workOrderService.deleteWorkOrder(id);
        return success(true);
    }

    @GetMapping("/get")
    @Operation(summary = "获得工单")
    @Parameter(name = "id", description = "工单编号", required = true, example = "1")
    @PreAuthorize("@ss.hasPermission('workorder:work-order:query')")
    public CommonResult<WorkOrderRespVO> getWorkOrder(@RequestParam("id") Long id) {
        WorkOrderDO workOrder = workOrderService.getWorkOrder(id);
        return success(buildWorkOrderDetail(workOrder));
    }

    @GetMapping("/page")
    @Operation(summary = "获得工单分页")
    @PreAuthorize("@ss.hasPermission('workorder:work-order:query')")
    public CommonResult<PageResult<WorkOrderRespVO>> getWorkOrderPage(@Valid WorkOrderPageReqVO pageVO) {
        PageResult<WorkOrderDO> pageResult = workOrderService.getWorkOrderPage(pageVO);
        return success(new PageResult<>(buildWorkOrderDetailList(pageResult.getList()), pageResult.getTotal()));
    }

    @GetMapping("/export-excel")
    @Operation(summary = "导出工单 Excel")
    @PreAuthorize("@ss.hasPermission('workorder:work-order:export')")
    public void exportWorkOrderExcel(@Valid WorkOrderPageReqVO exportReqVO,
                                      HttpServletResponse response) throws IOException {
        exportReqVO.setPageSize(PAGE_SIZE_NONE);
        List<WorkOrderDO> list = workOrderService.getWorkOrderPage(exportReqVO).getList();
        ExcelUtils.write(response, "工单.xls", "数据", WorkOrderRespVO.class,
                buildWorkOrderDetailList(list));
    }

    // ======================= 详情构建 =======================

    private WorkOrderRespVO buildWorkOrderDetail(WorkOrderDO workOrder) {
        if (workOrder == null) {
            return null;
        }
        return buildWorkOrderDetailList(Collections.singletonList(workOrder)).get(0);
    }

    public List<WorkOrderRespVO> buildWorkOrderDetailList(List<WorkOrderDO> list) {
        if (CollUtil.isEmpty(list)) {
            return Collections.emptyList();
        }
        // 1.1 获取工单类型列表
        Map<Long, WorkOrderTypeDO> typeMap = workOrderTypeService.getWorkOrderTypeMap(
                convertSet(list, WorkOrderDO::getTypeId));
        // 1.2 获取处理人和发起人列表
        Map<Long, AdminUserRespDTO> userMap = adminUserApi.getUserMap(
                convertListByFlatMap(list, wo -> {
                    if (wo.getHandlerUserId() != null && wo.getSubmitterUserId() != null) {
                        return java.util.stream.Stream.of(wo.getHandlerUserId(), wo.getSubmitterUserId());
                    }
                    if (wo.getHandlerUserId() != null) {
                        return java.util.stream.Stream.of(wo.getHandlerUserId());
                    }
                    if (wo.getSubmitterUserId() != null) {
                        return java.util.stream.Stream.of(wo.getSubmitterUserId());
                    }
                    return java.util.stream.Stream.empty();
                }));

        // 2. 拼接数据
        return BeanUtils.toBean(list, WorkOrderRespVO.class, workOrderVO -> {
            // 2.1 设置工单类型名称
            if (workOrderVO.getTypeId() != null) {
                WorkOrderTypeDO type = typeMap.get(workOrderVO.getTypeId());
                if (type != null) {
                    workOrderVO.setTypeName(type.getName());
                }
            }
            // 2.2 设置优先级名称
            if (workOrderVO.getPriority() != null) {
                WorkOrderPriorityEnum priorityEnum = WorkOrderPriorityEnum.fromPriority(workOrderVO.getPriority());
                if (priorityEnum != null) {
                    workOrderVO.setPriorityName(priorityEnum.getName());
                }
            }
            // 2.3 设置状态名称
            if (workOrderVO.getStatus() != null) {
                WorkOrderStatusEnum statusEnum = WorkOrderStatusEnum.fromStatus(workOrderVO.getStatus());
                if (statusEnum != null) {
                    workOrderVO.setStatusName(statusEnum.getName());
                }
            }
            // 2.4 设置处理人名称
            if (workOrderVO.getHandlerUserId() != null) {
                AdminUserRespDTO user = userMap.get(workOrderVO.getHandlerUserId());
                if (user != null) {
                    workOrderVO.setHandlerUserName(user.getNickname());
                }
            }
            // 2.5 设置发起人名称
            if (workOrderVO.getSubmitterUserId() != null) {
                AdminUserRespDTO user = userMap.get(workOrderVO.getSubmitterUserId());
                if (user != null) {
                    workOrderVO.setSubmitterUserName(user.getNickname());
                }
            }
        });
    }

}
