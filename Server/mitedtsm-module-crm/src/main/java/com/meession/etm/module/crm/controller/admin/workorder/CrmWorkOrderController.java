package com.meession.etm.module.crm.controller.admin.workorder;

import com.meession.etm.framework.apilog.core.annotation.ApiAccessLog;
import com.meession.etm.framework.common.pojo.CommonResult;
import com.meession.etm.framework.common.pojo.PageResult;
import com.meession.etm.framework.common.util.object.BeanUtils;
import com.meession.etm.framework.excel.core.util.ExcelUtils;
import com.meession.etm.framework.security.core.service.SecurityFrameworkService;
import com.meession.etm.module.crm.controller.admin.workorder.vo.*;
import com.meession.etm.module.crm.dal.dataobject.customer.CrmCustomerDO;
import com.meession.etm.module.crm.dal.dataobject.workorder.CrmWorkOrderDO;
import com.meession.etm.module.crm.dal.dataobject.workorder.CrmWorkOrderRecordDO;
import com.meession.etm.module.crm.service.customer.CrmCustomerService;
import com.meession.etm.module.crm.service.workorder.CrmWorkOrderService;
import com.meession.etm.module.crm.service.workorder.CrmWorkOrderGroupService;
import com.meession.etm.module.crm.framework.workorder.CrmWorkOrderDispatchProperties;
import com.meession.etm.module.system.api.user.AdminUserApi;
import com.meession.etm.module.system.api.user.dto.AdminUserRespDTO;
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
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.meession.etm.framework.common.pojo.CommonResult.success;
import static com.meession.etm.framework.apilog.core.enums.OperateTypeEnum.EXPORT;
import static com.meession.etm.framework.common.pojo.PageParam.PAGE_SIZE_NONE;
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
    @Resource private CrmWorkOrderGroupService groupService;
    @Resource private CrmWorkOrderDispatchProperties dispatchProperties;

    @PostMapping("/create")
    @Operation(summary = "创建客服工单")
    @PreAuthorize("@ss.hasPermission('crm:work-order:create')")
    public CommonResult<Long> create(@Valid @RequestBody CrmWorkOrderSaveReqVO reqVO) {
        return success(workOrderService.createWorkOrder(reqVO, getLoginUserId(),
                securityFrameworkService.hasPermission("crm:work-order:assign"),
                securityFrameworkService.hasPermission("crm:work-order:assign-all")));
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

    @GetMapping("/export-excel")
    @Operation(summary = "导出客服工单 Excel")
    @PreAuthorize("@ss.hasPermission('crm:work-order:export')")
    @ApiAccessLog(operateType = EXPORT)
    public void exportExcel(@Valid CrmWorkOrderPageReqVO reqVO, HttpServletResponse response) throws IOException {
        reqVO.setPageSize(PAGE_SIZE_NONE);
        Long userId = getLoginUserId();
        boolean queryAll = securityFrameworkService.hasPermission("crm:work-order:query-all");
        List<CrmWorkOrderRespVO> rows = build(workOrderService.getWorkOrderPage(reqVO, userId, queryAll).getList());
        ExcelUtils.write(response, "客服工单.xls", "工单",
                CrmWorkOrderExportRespVO.class, rows.stream().map(CrmWorkOrderExportRespVO::from).toList());
    }

    @PutMapping("/start")
    @Operation(summary = "开始处理客服工单")
    @PreAuthorize("@ss.hasPermission('crm:work-order:process')")
    public CommonResult<Boolean> start(@Valid @RequestBody CrmWorkOrderActionReqVO reqVO) {
        workOrderService.startWorkOrder(reqVO, getLoginUserId());
        return success(true);
    }

    @PutMapping("/assign")
    @Operation(summary = "分派待处理客服工单")
    @PreAuthorize("@ss.hasPermission('crm:work-order:assign')")
    public CommonResult<Boolean> assign(@Valid @RequestBody CrmWorkOrderAssignReqVO reqVO) {
        workOrderService.assignWorkOrder(reqVO, getLoginUserId(),
                securityFrameworkService.hasPermission("crm:work-order:assign-all"));
        return success(true);
    }

    @PutMapping("/claim")
    @Operation(summary = "领取处理组未分配工单")
    @PreAuthorize("@ss.hasPermission('crm:work-order:process')")
    public CommonResult<Boolean> claim(@Valid @RequestBody CrmWorkOrderActionReqVO reqVO) {
        workOrderService.claimWorkOrder(reqVO, getLoginUserId());
        return success(true);
    }

    @GetMapping("/dispatch-context")
    @Operation(summary = "获得工单派单策略、处理组和候选人")
    @PreAuthorize("@ss.hasAnyPermissions('crm:work-order:create', 'crm:work-order:assign')")
    public CommonResult<CrmWorkOrderDispatchContextRespVO> dispatchContext(@RequestParam Integer type,
                                                                           @RequestParam(required = false) Long groupId) {
        Long userId = getLoginUserId();
        boolean canAssign = securityFrameworkService.hasPermission("crm:work-order:assign");
        boolean assignAll = securityFrameworkService.hasPermission("crm:work-order:assign-all");
        List<CrmWorkOrderGroupRespVO> groups = buildGroups(groupService.getGroupList().stream()
                .filter(group -> Integer.valueOf(0).equals(group.getStatus()))
                .filter(group -> group.getSupportedTypes() != null && group.getSupportedTypes().contains(type)).toList());
        List<Long> candidateIds = canAssign
                ? workOrderService.getDispatchCandidateUserIds(type, groupId, userId, assignAll) : List.of();
        Map<Long, AdminUserRespDTO> users = adminUserApi.getUserMap(candidateIds);
        List<CrmWorkOrderDispatchContextRespVO.User> candidates = candidateIds.stream().map(id -> {
            AdminUserRespDTO user = users.get(id);
            if (user == null) return null;
            return new CrmWorkOrderDispatchContextRespVO.User().setId(id).setNickname(user.getNickname())
                    .setDeptId(user.getDeptId()).setSource(groupId != null ? "GROUP" : assignAll ? "ALL_GROUPS" : "SUBORDINATE")
                    .setOpenCount(workOrderService.getOpenWorkOrderCount(id));
        }).filter(Objects::nonNull).toList();
        return success(new CrmWorkOrderDispatchContextRespVO().setEnabled(dispatchProperties.isEnabled())
                .setAutoAssignOnCreate(dispatchProperties.isAutoAssignOnCreate())
                .setFallbackMode(dispatchProperties.getFallbackMode().name())
                .setMaxCcUsers(dispatchProperties.getMaxCcUsers()).setManualAssignmentAllowed(canAssign)
                .setGroups(groups).setCandidates(candidates));
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

    @PutMapping("/check-in")
    @Operation(summary = "移动签到客服工单")
    @PreAuthorize("@ss.hasPermission('crm:work-order:check-in')")
    public CommonResult<CrmWorkOrderCheckInRespVO> checkIn(@Valid @RequestBody CrmWorkOrderCheckInReqVO reqVO) {
        return success(BeanUtils.toBean(workOrderService.checkInWorkOrder(reqVO, getLoginUserId()),
                CrmWorkOrderCheckInRespVO.class));
    }

    @GetMapping("/check-in/latest")
    @Operation(summary = "获得工单最近一次签到")
    @PreAuthorize("@ss.hasPermission('crm:work-order:query')")
    public CommonResult<CrmWorkOrderCheckInRespVO> latestCheckIn(@RequestParam Long id) {
        Long userId = getLoginUserId();
        return success(BeanUtils.toBean(workOrderService.getLatestCheckIn(id, userId,
                securityFrameworkService.hasPermission("crm:work-order:query-all")), CrmWorkOrderCheckInRespVO.class));
    }

    @GetMapping("/sla")
    @Operation(summary = "获得工单 SLA")
    @PreAuthorize("@ss.hasPermission('crm:work-order:query')")
    public CommonResult<CrmWorkOrderSlaRespVO> sla(@RequestParam Long id) {
        Long userId = getLoginUserId();
        var sla = workOrderService.getWorkOrderSla(id, userId,
                securityFrameworkService.hasPermission("crm:work-order:query-all"));
        CrmWorkOrderSlaRespVO response = BeanUtils.toBean(sla, CrmWorkOrderSlaRespVO.class);
        if (response != null) {
            workOrderService.getSlaPolicies().stream().filter(policy -> Objects.equals(policy.getId(), sla.getPolicyId()))
                    .findFirst().ifPresent(policy -> response.setPolicyCode(policy.getCode()).setPolicyName(policy.getName()));
            response.setPaused(sla.getPausedAt() != null).setOverdue(Integer.valueOf(3).equals(sla.getStatus()));
        }
        return success(response);
    }

    @PutMapping("/sla/pause")
    @Operation(summary = "暂停工单 SLA")
    @PreAuthorize("@ss.hasPermission('crm:work-order:sla-action')")
    public CommonResult<Boolean> pauseSla(@Valid @RequestBody CrmWorkOrderSlaActionReqVO reqVO) {
        workOrderService.pauseWorkOrderSla(reqVO, getLoginUserId());
        return success(true);
    }

    @PutMapping("/sla/resume")
    @Operation(summary = "恢复工单 SLA")
    @PreAuthorize("@ss.hasPermission('crm:work-order:sla-action')")
    public CommonResult<Boolean> resumeSla(@Valid @RequestBody CrmWorkOrderSlaActionReqVO reqVO) {
        workOrderService.resumeWorkOrderSla(reqVO, getLoginUserId());
        return success(true);
    }

    @GetMapping("/sla/policies")
    @Operation(summary = "获得工单 SLA 策略")
    @PreAuthorize("@ss.hasPermission('crm:work-order:sla-admin')")
    public CommonResult<List<CrmWorkOrderSlaPolicyRespVO>> slaPolicies() {
        return success(BeanUtils.toBean(workOrderService.getSlaPolicies(), CrmWorkOrderSlaPolicyRespVO.class));
    }

    @GetMapping("/sla/holidays")
    @Operation(summary = "获得工单工作日历")
    @PreAuthorize("@ss.hasPermission('crm:work-order:sla-admin')")
    public CommonResult<List<CrmWorkOrderHolidayRespVO>> holidays() {
        return success(BeanUtils.toBean(workOrderService.getHolidays(), CrmWorkOrderHolidayRespVO.class));
    }

    @PostMapping("/sla/holiday/save")
    @Operation(summary = "保存工单工作日历")
    @PreAuthorize("@ss.hasPermission('crm:work-order:sla-admin')")
    public CommonResult<Long> saveHoliday(@Valid @RequestBody CrmWorkOrderHolidaySaveReqVO reqVO) {
        return success(workOrderService.saveHoliday(reqVO, getLoginUserId()));
    }

    @DeleteMapping("/sla/holiday/delete")
    @Operation(summary = "删除工单工作日历")
    @PreAuthorize("@ss.hasPermission('crm:work-order:sla-admin')")
    public CommonResult<Boolean> deleteHoliday(@RequestParam Long id) {
        workOrderService.deleteHoliday(id, getLoginUserId());
        return success(true);
    }

    private List<CrmWorkOrderRespVO> build(List<CrmWorkOrderDO> orders) {
        if (orders == null || orders.isEmpty()) return Collections.emptyList();
        Set<Long> customerIds = orders.stream().map(CrmWorkOrderDO::getCustomerId)
                .filter(Objects::nonNull).collect(Collectors.toSet());
        Map<Long, CrmCustomerDO> customers = customerService.getCustomerMap(customerIds);
        Map<Long, List<Long>> ccMap = workOrderService.getCcUserIdsMap(orders.stream().map(CrmWorkOrderDO::getId).toList());
        Map<Long, com.meession.etm.module.crm.dal.dataobject.workorder.CrmWorkOrderGroupDO> groupMap =
                groupService.getGroupMap(orders.stream().map(CrmWorkOrderDO::getGroupId).filter(Objects::nonNull).toList());
        Set<Long> userIds = orders.stream().flatMap(order -> Stream.of(
                        order.getHandlerUserId(), parseUserId(order.getCreator())))
                .filter(Objects::nonNull).collect(Collectors.toSet());
        ccMap.values().forEach(userIds::addAll);
        Map<Long, AdminUserRespDTO> users = adminUserApi.getUserMap(userIds);
        return BeanUtils.toBean(orders, CrmWorkOrderRespVO.class, vo -> {
            Optional.ofNullable(customers.get(vo.getCustomerId())).ifPresent(c -> vo.setCustomerName(c.getName()));
            if (vo.getHandlerUserId() != null) {
                Optional.ofNullable(users.get(vo.getHandlerUserId())).ifPresent(u -> vo.setHandlerUserName(u.getNickname()));
            }
            Long creatorId = parseUserId(vo.getCreator());
            if (creatorId != null) Optional.ofNullable(users.get(creatorId)).ifPresent(u -> vo.setCreatorName(u.getNickname()));
            if (vo.getGroupId() != null) {
                Optional.ofNullable(groupMap.get(vo.getGroupId())).ifPresent(group -> vo.setGroupName(group.getName()));
            }
            List<Long> ccIds = ccMap.getOrDefault(vo.getId(), List.of());
            vo.setCcUserIds(ccIds);
            vo.setCcUserNames(ccIds.stream().map(users::get).filter(Objects::nonNull).map(AdminUserRespDTO::getNickname).toList());
        });
    }

    private List<CrmWorkOrderGroupRespVO> buildGroups(List<com.meession.etm.module.crm.dal.dataobject.workorder.CrmWorkOrderGroupDO> groups) {
        Map<Long, List<Long>> memberMap = groupService.getMemberUserIdsMap(groups.stream().map(com.meession.etm.module.crm.dal.dataobject.workorder.CrmWorkOrderGroupDO::getId).toList());
        return BeanUtils.toBean(groups, CrmWorkOrderGroupRespVO.class, vo ->
                vo.setMemberUserIds(memberMap.getOrDefault(vo.getId(), List.of())));
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
