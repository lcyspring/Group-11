package com.meession.etm.module.bpm.controller.admin.oa;

import com.meession.etm.framework.common.pojo.CommonResult;
import com.meession.etm.module.bpm.controller.admin.oa.vo.BpmOAEventSaveReqVO;
import com.meession.etm.module.bpm.dal.dataobject.oa.BpmOAEventDO;
import com.meession.etm.module.bpm.service.oa.BpmOAEventService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDateTime;
import java.util.List;
import static com.meession.etm.framework.common.pojo.CommonResult.success;
import static com.meession.etm.framework.security.core.util.SecurityFrameworkUtils.getLoginUserId;

@Tag(name = "管理后台 - OA 日程")
@RestController
@RequestMapping("/bpm/oa/event")
@Validated
public class BpmOAEventController {
    @Resource private BpmOAEventService service;
    @PostMapping("/create") @Operation(summary = "创建日程") @PreAuthorize("@ss.hasPermission('bpm:oa-event:create')")
    public CommonResult<Long> create(@Valid @RequestBody BpmOAEventSaveReqVO req) { return success(service.create(getLoginUserId(), req)); }
    @PutMapping("/update") @Operation(summary = "更新日程") @PreAuthorize("@ss.hasPermission('bpm:oa-event:update')")
    public CommonResult<Boolean> update(@Valid @RequestBody BpmOAEventSaveReqVO req) { service.update(getLoginUserId(), req); return success(true); }
    @DeleteMapping("/delete") @Operation(summary = "删除日程") @PreAuthorize("@ss.hasPermission('bpm:oa-event:delete')")
    public CommonResult<Boolean> delete(@RequestParam Long id) { service.delete(getLoginUserId(), id); return success(true); }
    @GetMapping("/get") @Operation(summary = "获得日程") @PreAuthorize("@ss.hasPermission('bpm:oa-event:query')")
    public CommonResult<BpmOAEventDO> get(@RequestParam Long id) { return success(service.get(getLoginUserId(), id)); }
    @GetMapping("/list") @Operation(summary = "获得时间范围内日程") @PreAuthorize("@ss.hasPermission('bpm:oa-event:query')")
    public CommonResult<List<BpmOAEventDO>> list(
            @RequestParam(required = false) LocalDateTime from,
            @RequestParam(required = false) LocalDateTime to) {
        return success(service.list(getLoginUserId(), from, to));
    }
}
