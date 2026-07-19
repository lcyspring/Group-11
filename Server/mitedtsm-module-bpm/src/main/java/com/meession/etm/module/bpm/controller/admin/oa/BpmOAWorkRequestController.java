package com.meession.etm.module.bpm.controller.admin.oa;

import com.meession.etm.framework.common.pojo.CommonResult;
import com.meession.etm.module.bpm.controller.admin.oa.vo.BpmOAWorkRequestCreateReqVO;
import com.meession.etm.module.bpm.dal.dataobject.oa.BpmOAWorkRequestDO;
import com.meession.etm.module.bpm.service.oa.BpmOAWorkRequestService;
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

@Tag(name = "管理后台 - OA 请示") @RestController @RequestMapping("/bpm/oa/work-request") @Validated
public class BpmOAWorkRequestController {
    @Resource private BpmOAWorkRequestService service;
    @PostMapping("/create") @Operation(summary = "发起请示") @PreAuthorize("@ss.hasPermission('bpm:oa-work-request:create')")
    public CommonResult<Long> create(@Valid @RequestBody BpmOAWorkRequestCreateReqVO req) { return success(service.create(getLoginUserId(), req)); }
    @GetMapping("/get") @Operation(summary = "获得请示") @PreAuthorize("@ss.hasPermission('bpm:oa-work-request:query')")
    public CommonResult<BpmOAWorkRequestDO> get(@RequestParam Long id) { return success(service.get(getLoginUserId(), id)); }
    @GetMapping("/list") @Operation(summary = "请示列表") @PreAuthorize("@ss.hasPermission('bpm:oa-work-request:query')")
    public CommonResult<List<BpmOAWorkRequestDO>> list() { return success(service.list(getLoginUserId())); }
}
