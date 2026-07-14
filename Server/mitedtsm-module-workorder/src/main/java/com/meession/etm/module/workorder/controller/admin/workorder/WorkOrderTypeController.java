package com.meession.etm.module.workorder.controller.admin.workorder;

import com.meession.etm.framework.common.pojo.CommonResult;
import com.meession.etm.framework.common.pojo.PageResult;
import com.meession.etm.framework.common.util.object.BeanUtils;
import com.meession.etm.module.workorder.controller.admin.workorder.vo.type.*;
import com.meession.etm.module.workorder.dal.dataobject.WorkOrderTypeDO;
import com.meession.etm.module.workorder.service.WorkOrderTypeService;
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

@Tag(name = "管理后台 - 工单类型管理")
@RestController
@RequestMapping("/workorder/work-order-type")
@Validated
public class WorkOrderTypeController {

    @Resource
    private WorkOrderTypeService workOrderTypeService;

    @PostMapping("/create")
    @Operation(summary = "创建工单类型")
    @PreAuthorize("@ss.hasPermission('workorder:work-order-type:create')")
    public CommonResult<Long> createWorkOrderType(@Valid @RequestBody WorkOrderTypeSaveReqVO createReqVO) {
        return success(workOrderTypeService.createWorkOrderType(createReqVO));
    }

    @PutMapping("/update")
    @Operation(summary = "更新工单类型")
    @PreAuthorize("@ss.hasPermission('workorder:work-order-type:update')")
    public CommonResult<Boolean> updateWorkOrderType(@Valid @RequestBody WorkOrderTypeSaveReqVO updateReqVO) {
        workOrderTypeService.updateWorkOrderType(updateReqVO);
        return success(true);
    }

    @DeleteMapping("/delete")
    @Operation(summary = "删除工单类型")
    @Parameter(name = "id", description = "编号", required = true)
    @PreAuthorize("@ss.hasPermission('workorder:work-order-type:delete')")
    public CommonResult<Boolean> deleteWorkOrderType(@RequestParam("id") Long id) {
        workOrderTypeService.deleteWorkOrderType(id);
        return success(true);
    }

    @GetMapping("/get")
    @Operation(summary = "获得工单类型")
    @Parameter(name = "id", description = "编号", required = true, example = "1")
    @PreAuthorize("@ss.hasPermission('workorder:work-order-type:query')")
    public CommonResult<WorkOrderTypeRespVO> getWorkOrderType(@RequestParam("id") Long id) {
        WorkOrderTypeDO type = workOrderTypeService.getWorkOrderType(id);
        return success(BeanUtils.toBean(type, WorkOrderTypeRespVO.class));
    }

    @GetMapping("/page")
    @Operation(summary = "获得工单类型分页")
    @PreAuthorize("@ss.hasPermission('workorder:work-order-type:query')")
    public CommonResult<PageResult<WorkOrderTypeRespVO>> getWorkOrderTypePage(@Valid WorkOrderTypePageReqVO pageVO) {
        PageResult<WorkOrderTypeDO> pageResult = workOrderTypeService.getWorkOrderTypePage(pageVO);
        return success(BeanUtils.toBean(pageResult, WorkOrderTypeRespVO.class));
    }

    @GetMapping("/list-all")
    @Operation(summary = "获得所有启用的工单类型列表")
    @PreAuthorize("@ss.hasPermission('workorder:work-order-type:query')")
    public CommonResult<List<WorkOrderTypeRespVO>> getEnableWorkOrderTypeList() {
        List<WorkOrderTypeDO> list = workOrderTypeService.getEnableWorkOrderTypeList();
        return success(BeanUtils.toBean(list, WorkOrderTypeRespVO.class));
    }

}
