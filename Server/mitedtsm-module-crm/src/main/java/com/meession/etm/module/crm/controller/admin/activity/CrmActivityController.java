package com.meession.etm.module.crm.controller.admin.activity;

import com.meession.etm.framework.common.pojo.CommonResult;
import com.meession.etm.framework.common.pojo.PageResult;
import com.meession.etm.framework.common.util.number.NumberUtils;
import com.meession.etm.framework.common.util.object.BeanUtils;
import com.meession.etm.module.crm.controller.admin.activity.vo.*;
import com.meession.etm.module.crm.dal.dataobject.activity.*;
import com.meession.etm.module.crm.service.activity.CrmActivityService;
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

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.meession.etm.framework.common.pojo.CommonResult.success;
import static com.meession.etm.framework.security.core.util.SecurityFrameworkUtils.getLoginUserId;

@Tag(name = "管理后台 - CRM 活动")
@RestController
@RequestMapping("/crm/activity")
@Validated
public class CrmActivityController {

    @Resource
    private CrmActivityService activityService;
    @Resource
    private AdminUserApi adminUserApi;

    @PostMapping("/task/create")
    @Operation(summary = "创建 CRM 任务")
    @PreAuthorize("@ss.hasPermission('crm:activity:create')")
    public CommonResult<Long> createTask(@Valid @RequestBody CrmTaskSaveReqVO reqVO) {
        return success(activityService.createTask(reqVO, getLoginUserId()));
    }

    @PutMapping("/task/update")
    @Operation(summary = "修改未开始的 CRM 任务")
    @PreAuthorize("@ss.hasPermission('crm:activity:update')")
    public CommonResult<Boolean> updateTask(@Valid @RequestBody CrmTaskSaveReqVO reqVO) {
        activityService.updateTask(reqVO, getLoginUserId());
        return success(true);
    }

    @PutMapping("/task/start")
    @Operation(summary = "开始 CRM 任务")
    @PreAuthorize("@ss.hasPermission('crm:activity:update')")
    public CommonResult<Boolean> startTask(@Valid @RequestBody CrmTaskActionReqVO reqVO) {
        activityService.startTask(reqVO, getLoginUserId());
        return success(true);
    }

    @PutMapping("/task/complete")
    @Operation(summary = "完成 CRM 任务")
    @PreAuthorize("@ss.hasPermission('crm:activity:update')")
    public CommonResult<Boolean> completeTask(@Valid @RequestBody CrmTaskActionReqVO reqVO) {
        activityService.completeTask(reqVO, getLoginUserId());
        return success(true);
    }

    @PutMapping("/task/unable")
    @Operation(summary = "将 CRM 任务标记为未完成")
    @PreAuthorize("@ss.hasPermission('crm:activity:update')")
    public CommonResult<Boolean> markTaskUnable(@Valid @RequestBody CrmTaskActionReqVO reqVO) {
        activityService.markTaskUnable(reqVO, getLoginUserId());
        return success(true);
    }

    @PutMapping("/task/cancel")
    @Operation(summary = "取消 CRM 任务")
    @PreAuthorize("@ss.hasPermission('crm:activity:update')")
    public CommonResult<Boolean> cancelTask(@Valid @RequestBody CrmTaskActionReqVO reqVO) {
        activityService.cancelTask(reqVO, getLoginUserId());
        return success(true);
    }

    @GetMapping("/task/page")
    @Operation(summary = "获得 CRM 任务分页")
    @PreAuthorize("@ss.hasPermission('crm:activity:query')")
    public CommonResult<PageResult<CrmTaskRespVO>> getTaskPage(@Valid CrmTaskPageReqVO reqVO) {
        PageResult<CrmTaskDO> page = activityService.getTaskPage(reqVO);
        return success(new PageResult<>(buildTasks(page.getList()), page.getTotal()));
    }

    @GetMapping("/task/action-records")
    @Operation(summary = "获得 CRM 任务不可变动作轨迹")
    @Parameter(name = "taskId", required = true)
    @PreAuthorize("@ss.hasPermission('crm:activity:query')")
    public CommonResult<List<CrmTaskActionRecordRespVO>> getTaskActionRecords(@RequestParam Long taskId) {
        List<CrmTaskActionRecordDO> records = activityService.getTaskActionRecords(taskId, getLoginUserId());
        Set<Long> userIds = records.stream().map(CrmTaskActionRecordDO::getOperatorUserId)
                .filter(Objects::nonNull).collect(Collectors.toSet());
        Map<Long, AdminUserRespDTO> users = getUsers(userIds);
        return success(BeanUtils.toBean(records, CrmTaskActionRecordRespVO.class, item -> {
            AdminUserRespDTO user = users.get(item.getOperatorUserId());
            if (user != null) item.setOperatorUserName(user.getNickname());
        }));
    }

    @PostMapping("/call/create")
    @Operation(summary = "创建 CRM 通话记录")
    @PreAuthorize("@ss.hasPermission('crm:activity:create')")
    public CommonResult<Long> createCallRecord(@Valid @RequestBody CrmCallRecordSaveReqVO reqVO) {
        return success(activityService.createCallRecord(reqVO, getLoginUserId()));
    }

    @GetMapping("/call/page")
    @Operation(summary = "获得 CRM 通话记录分页")
    @PreAuthorize("@ss.hasPermission('crm:activity:query')")
    public CommonResult<PageResult<CrmCallRecordRespVO>> getCallRecordPage(@Valid CrmActivityPageReqVO reqVO) {
        PageResult<CrmCallRecordDO> page = activityService.getCallRecordPage(reqVO);
        Set<Long> userIds = page.getList().stream().map(CrmCallRecordDO::getOperatorUserId)
                .filter(Objects::nonNull).collect(Collectors.toSet());
        Map<Long, AdminUserRespDTO> users = getUsers(userIds);
        List<CrmCallRecordRespVO> list = BeanUtils.toBean(page.getList(), CrmCallRecordRespVO.class, item -> {
            AdminUserRespDTO user = users.get(item.getOperatorUserId());
            if (user != null) item.setOperatorUserName(user.getNickname());
        });
        return success(new PageResult<>(list, page.getTotal()));
    }

    @PostMapping("/sms/create")
    @Operation(summary = "创建 CRM 短信记录")
    @PreAuthorize("@ss.hasPermission('crm:activity:create')")
    public CommonResult<Long> createSmsRecord(@Valid @RequestBody CrmSmsRecordSaveReqVO reqVO) {
        return success(activityService.createSmsRecord(reqVO, getLoginUserId()));
    }

    @GetMapping("/sms/page")
    @Operation(summary = "获得 CRM 短信记录分页")
    @PreAuthorize("@ss.hasPermission('crm:activity:query')")
    public CommonResult<PageResult<CrmSmsRecordRespVO>> getSmsRecordPage(@Valid CrmActivityPageReqVO reqVO) {
        PageResult<CrmSmsRecordDO> page = activityService.getSmsRecordPage(reqVO);
        Set<Long> userIds = page.getList().stream().map(CrmSmsRecordDO::getOperatorUserId)
                .filter(Objects::nonNull).collect(Collectors.toSet());
        Map<Long, AdminUserRespDTO> users = getUsers(userIds);
        List<CrmSmsRecordRespVO> list = BeanUtils.toBean(page.getList(), CrmSmsRecordRespVO.class, item -> {
            AdminUserRespDTO user = users.get(item.getOperatorUserId());
            if (user != null) item.setOperatorUserName(user.getNickname());
        });
        return success(new PageResult<>(list, page.getTotal()));
    }

    @GetMapping("/conversion-record")
    @Operation(summary = "获得线索转换活动迁移审计")
    @PreAuthorize("@ss.hasPermission('crm:activity:query')")
    public CommonResult<CrmClueConversionRecordRespVO> getConversionRecord(@RequestParam Long clueId) {
        CrmClueConversionRecordDO record = activityService.getConversionRecord(clueId);
        if (record == null) return success(null);
        CrmClueConversionRecordRespVO response = BeanUtils.toBean(record, CrmClueConversionRecordRespVO.class);
        AdminUserRespDTO user = getUsers(Collections.singleton(record.getOperatorUserId()))
                .get(record.getOperatorUserId());
        if (user != null) response.setOperatorUserName(user.getNickname());
        return success(response);
    }

    private List<CrmTaskRespVO> buildTasks(List<CrmTaskDO> tasks) {
        Set<Long> userIds = tasks.stream().flatMap(task -> Stream.of(
                        task.getAssigneeUserId(), NumberUtils.parseLong(task.getCreator())))
                .filter(Objects::nonNull).collect(Collectors.toSet());
        Map<Long, AdminUserRespDTO> users = getUsers(userIds);
        return BeanUtils.toBean(tasks, CrmTaskRespVO.class, item -> {
            AdminUserRespDTO assignee = users.get(item.getAssigneeUserId());
            if (assignee != null) item.setAssigneeUserName(assignee.getNickname());
            AdminUserRespDTO creator = users.get(NumberUtils.parseLong(item.getCreator()));
            if (creator != null) item.setCreatorName(creator.getNickname());
        });
    }

    private Map<Long, AdminUserRespDTO> getUsers(Set<Long> userIds) {
        Set<Long> filtered = userIds.stream().filter(Objects::nonNull).collect(Collectors.toSet());
        return filtered.isEmpty() ? Collections.emptyMap() : adminUserApi.getUserMap(filtered);
    }
}
