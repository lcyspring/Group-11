package com.meession.etm.module.bpm.controller.admin.oa;

import com.meession.etm.framework.common.pojo.CommonResult;
import com.meession.etm.module.bpm.controller.admin.oa.vo.BpmOATaskSaveReqVO;
import com.meession.etm.module.bpm.dal.dataobject.oa.BpmOATaskDO;
import com.meession.etm.module.bpm.service.oa.BpmOATaskService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import static com.meession.etm.framework.common.pojo.CommonResult.success;
import static com.meession.etm.framework.security.core.util.SecurityFrameworkUtils.getLoginUserId;

@Tag(name = "管理后台 - OA 任务")
@RestController @RequestMapping("/bpm/oa/task") @Validated
public class BpmOATaskController {
    @Resource private BpmOATaskService service;
    @PostMapping("/create") @Operation(summary = "创建 OA 任务") @PreAuthorize("@ss.hasPermission('bpm:oa-task:create')")
    public CommonResult<Long> create(@Valid @RequestBody BpmOATaskSaveReqVO req) { return success(service.create(getLoginUserId(), req)); }
    @PutMapping("/update") @Operation(summary = "更新 OA 任务") @PreAuthorize("@ss.hasPermission('bpm:oa-task:update')")
    public CommonResult<Boolean> update(@Valid @RequestBody BpmOATaskSaveReqVO req) { service.update(getLoginUserId(), req); return success(true); }
    @DeleteMapping("/delete") @Operation(summary = "删除 OA 任务") @PreAuthorize("@ss.hasPermission('bpm:oa-task:delete')")
    public CommonResult<Boolean> delete(@RequestParam Long id) { service.delete(getLoginUserId(), id); return success(true); }
    @GetMapping("/get") @Operation(summary = "获得 OA 任务") @PreAuthorize("@ss.hasPermission('bpm:oa-task:query')")
    public CommonResult<BpmOATaskDO> get(@RequestParam Long id) { return success(service.get(getLoginUserId(), id)); }
    @GetMapping("/list") @Operation(summary = "获得 OA 任务列表") @PreAuthorize("@ss.hasPermission('bpm:oa-task:query')")
    public CommonResult<List<BpmOATaskDO>> list(@RequestParam(required = false) Integer status) { return success(service.list(getLoginUserId(), status)); }
    @PutMapping("/start") @Operation(summary = "开始 OA 任务") @PreAuthorize("@ss.hasPermission('bpm:oa-task:update')")
    public CommonResult<Boolean> start(@RequestParam Long id) { service.start(getLoginUserId(), id); return success(true); }
    @PutMapping("/complete") @Operation(summary = "完成 OA 任务") @PreAuthorize("@ss.hasPermission('bpm:oa-task:update')")
    public CommonResult<Boolean> complete(@RequestParam Long id, @RequestParam(required = false) String result) { service.complete(getLoginUserId(), id, result); return success(true); }
}
