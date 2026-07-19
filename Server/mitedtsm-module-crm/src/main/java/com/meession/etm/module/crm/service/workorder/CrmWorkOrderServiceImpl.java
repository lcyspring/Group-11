package com.meession.etm.module.crm.service.workorder;

import cn.hutool.core.util.ObjectUtil;
import com.meession.etm.framework.common.pojo.PageResult;
import com.meession.etm.framework.common.util.object.BeanUtils;
import com.meession.etm.module.crm.controller.admin.workorder.vo.*;
import com.meession.etm.module.crm.dal.dataobject.business.CrmBusinessDO;
import com.meession.etm.module.crm.dal.dataobject.contract.CrmContractDO;
import com.meession.etm.module.crm.dal.dataobject.workorder.CrmWorkOrderDO;
import com.meession.etm.module.crm.dal.dataobject.workorder.CrmWorkOrderCcDO;
import com.meession.etm.module.crm.dal.dataobject.workorder.CrmWorkOrderGroupDO;
import com.meession.etm.module.crm.dal.dataobject.workorder.CrmWorkOrderRecordDO;
import com.meession.etm.module.crm.dal.mysql.workorder.CrmWorkOrderCcMapper;
import com.meession.etm.module.crm.dal.mysql.workorder.CrmWorkOrderMapper;
import com.meession.etm.module.crm.dal.mysql.workorder.CrmWorkOrderRecordMapper;
import com.meession.etm.module.crm.dal.mysql.workorder.CrmWorkOrderCheckInMapper;
import com.meession.etm.module.crm.dal.mysql.workorder.CrmWorkOrderHolidayMapper;
import com.meession.etm.module.crm.dal.mysql.workorder.CrmWorkOrderSlaMapper;
import com.meession.etm.module.crm.dal.mysql.workorder.CrmWorkOrderSlaPolicyMapper;
import com.meession.etm.module.crm.dal.dataobject.workorder.CrmWorkOrderCheckInDO;
import com.meession.etm.module.crm.dal.dataobject.workorder.CrmWorkOrderHolidayDO;
import com.meession.etm.module.crm.dal.dataobject.workorder.CrmWorkOrderSlaDO;
import com.meession.etm.module.crm.dal.dataobject.workorder.CrmWorkOrderSlaPolicyDO;
import com.meession.etm.module.crm.dal.redis.no.CrmNoRedisDAO;
import com.meession.etm.module.crm.enums.workorder.*;
import com.meession.etm.module.crm.framework.workorder.CrmWorkOrderDispatchProperties;
import com.meession.etm.module.crm.framework.workorder.CrmWorkOrderGovernanceProperties;
import com.meession.etm.module.crm.service.business.CrmBusinessService;
import com.meession.etm.module.crm.service.contract.CrmContractService;
import com.meession.etm.module.crm.service.customer.CrmCustomerService;
import com.meession.etm.module.system.api.user.AdminUserApi;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static com.meession.etm.framework.common.exception.util.ServiceExceptionUtil.exception;
import static com.meession.etm.module.crm.enums.ErrorCodeConstants.*;

@Service
@Validated
public class CrmWorkOrderServiceImpl implements CrmWorkOrderService {

    @Resource private CrmWorkOrderMapper workOrderMapper;
    @Resource private CrmWorkOrderRecordMapper recordMapper;
    @Resource private CrmWorkOrderCcMapper ccMapper;
    @Resource private CrmNoRedisDAO noRedisDAO;
    @Resource private CrmCustomerService customerService;
    @Resource private CrmBusinessService businessService;
    @Resource private CrmContractService contractService;
    @Resource private AdminUserApi adminUserApi;
    @Resource private CrmWorkOrderNotificationService notificationService;
    @Resource private CrmWorkOrderGroupService groupService;
    @Resource private CrmWorkOrderDispatchProperties dispatchProperties;
    @Resource private CrmWorkOrderGovernanceProperties governanceProperties;
    @Resource private CrmWorkOrderCheckInMapper checkInMapper;
    @Resource private CrmWorkOrderSlaMapper slaMapper;
    @Resource private CrmWorkOrderSlaPolicyMapper slaPolicyMapper;
    @Resource private CrmWorkOrderHolidayMapper holidayMapper;
    @Resource private CrmWorkOrderSlaCalculator slaCalculator;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createWorkOrder(CrmWorkOrderSaveReqVO reqVO, Long userId, boolean canAssign, boolean assignAll) {
        validateRelations(reqVO.getCustomerId(), reqVO.getSourceType(), reqVO.getSourceId());
        validateDescription(reqVO.getDescription());
        validateServiceLocation(reqVO);
        validateCcUsers(reqVO.getCcUserIds());
        DispatchDecision dispatch = resolveCreateDispatch(reqVO.getType(), reqVO.getGroupId(),
                reqVO.getHandlerUserId(), userId, canAssign, assignAll);
        String no = noRedisDAO.generateMonthly(CrmNoRedisDAO.WORK_ORDER_PREFIX);
        if (workOrderMapper.selectByNo(no) != null) {
            throw exception(WORK_ORDER_NO_EXISTS);
        }
        CrmWorkOrderDO workOrder = BeanUtils.toBean(reqVO, CrmWorkOrderDO.class)
                .setNo(no).setStatus(CrmWorkOrderStatusEnum.PENDING.getStatus())
                .setGroupId(dispatch.groupId()).setHandlerUserId(dispatch.handlerUserId())
                .setDispatchMode(dispatch.mode()).setAssignTime(dispatch.handlerUserId() == null ? null : LocalDateTime.now());
        if (workOrder.getServiceLatitude() != null && workOrder.getGeofenceRadiusMeters() == null
                && governanceProperties != null) {
            workOrder.setGeofenceRadiusMeters(governanceProperties.getGeofence().getDefaultRadiusMeters());
        }
        workOrder.setCreator(String.valueOf(userId));
        workOrderMapper.insert(workOrder);
        initializeSla(workOrder);
        replaceCcUsers(workOrder.getId(), reqVO.getCcUserIds());
        appendRecord(workOrder, CrmWorkOrderActionTypeEnum.CREATE, null,
                CrmWorkOrderStatusEnum.PENDING.getStatus(), userId, null);
        notificationService.notifyAssigned(workOrder);
        notificationService.notifyCopied(workOrder, distinctIds(reqVO.getCcUserIds()));
        return workOrder.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateWorkOrder(CrmWorkOrderSaveReqVO reqVO, Long userId) {
        CrmWorkOrderDO old = requireParticipant(reqVO.getId(), userId, false);
        if (!ObjectUtil.equal(old.getCreator(), String.valueOf(userId))
                || !ObjectUtil.equal(old.getStatus(), CrmWorkOrderStatusEnum.PENDING.getStatus())
                && !ObjectUtil.equal(old.getStatus(), CrmWorkOrderStatusEnum.RETURNED.getStatus())) {
            throw exception(WORK_ORDER_CREATOR_ONLY);
        }
        // 来源、客户、处理人属于闭环主键，编辑阶段不可偷偷替换。
        validateRelations(old.getCustomerId(), old.getSourceType(), old.getSourceId());
        validateDescription(reqVO.getDescription());
        validateServiceLocation(reqVO);
        validateCcUsers(reqVO.getCcUserIds());
        Set<Long> previousCc = new LinkedHashSet<>(getCcUserIdsMap(List.of(old.getId()))
                .getOrDefault(old.getId(), List.of()));
        CrmWorkOrderDO update = BeanUtils.toBean(reqVO, CrmWorkOrderDO.class)
                .setId(old.getId()).setCustomerId(old.getCustomerId()).setSourceType(old.getSourceType())
                .setSourceId(old.getSourceId()).setGroupId(old.getGroupId()).setHandlerUserId(old.getHandlerUserId())
                .setDispatchMode(old.getDispatchMode()).setAssignTime(old.getAssignTime())
                .setStatus(old.getStatus());
        workOrderMapper.updateById(update);
        replaceCcUsers(old.getId(), reqVO.getCcUserIds());
        appendRecord(old, CrmWorkOrderActionTypeEnum.UPDATE, old.getStatus(), old.getStatus(), userId,
                "修改工单内容");
        Set<Long> addedCc = distinctIds(reqVO.getCcUserIds());
        addedCc.removeAll(previousCc);
        if (!addedCc.isEmpty()) {
            appendRecord(old, CrmWorkOrderActionTypeEnum.CC_UPDATE, old.getStatus(), old.getStatus(), userId,
                    "新增抄送人 " + addedCc.size() + " 名");
            notificationService.notifyCopied(old, addedCc);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteWorkOrder(Long id, Long userId) {
        CrmWorkOrderDO old = requireParticipant(id, userId, false);
        if (!ObjectUtil.equal(old.getCreator(), String.valueOf(userId))
                || !ObjectUtil.equal(old.getStatus(), CrmWorkOrderStatusEnum.PENDING.getStatus())) {
            throw exception(WORK_ORDER_DELETE_STATUS_INVALID);
        }
        recordMapper.deleteByWorkOrderId(id);
        ccMapper.deleteByWorkOrderId(id);
        workOrderMapper.deleteById(id);
    }

    @Override
    public CrmWorkOrderDO getWorkOrder(Long id, Long userId, boolean queryAll) {
        return requireParticipant(id, userId, queryAll);
    }

    @Override
    public PageResult<CrmWorkOrderDO> getWorkOrderPage(CrmWorkOrderPageReqVO reqVO, Long userId, boolean queryAll) {
        Set<Long> memberGroups = groupService.getMemberGroupIds(userId);
        Set<Long> managedGroups = groupService.getManagedGroupIds(userId);
        Set<Long> ccOrders = ccMapper.selectByUserId(userId).stream().map(CrmWorkOrderCcDO::getWorkOrderId)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        return workOrderMapper.selectPage(reqVO, userId, queryAll, managedGroups, memberGroups, ccOrders);
    }

    @Override
    public List<CrmWorkOrderRecordDO> getWorkOrderRecords(Long id, Long userId, boolean queryAll) {
        requireParticipant(id, userId, queryAll);
        return recordMapper.selectListByWorkOrderId(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void assignWorkOrder(CrmWorkOrderAssignReqVO reqVO, Long userId, boolean assignAll) {
        CrmWorkOrderDO old = workOrderMapper.selectById(reqVO.getId());
        if (old == null) throw exception(WORK_ORDER_NOT_EXISTS);
        boolean managesCurrentGroup = old.getGroupId() != null && groupService.isGroupManager(old.getGroupId(), userId);
        boolean ownsUngroupedOrder = old.getGroupId() == null
                && ObjectUtil.equal(old.getCreator(), String.valueOf(userId));
        if (!assignAll && !managesCurrentGroup && !ownsUngroupedOrder) {
            throw exception(WORK_ORDER_ASSIGN_DENIED);
        }
        Long targetGroupId = reqVO.getGroupId() == null ? old.getGroupId() : reqVO.getGroupId();
        if (ObjectUtil.equal(old.getGroupId(), targetGroupId)
                && ObjectUtil.equal(old.getHandlerUserId(), reqVO.getHandlerUserId())) {
            throw exception(WORK_ORDER_HANDLER_UNCHANGED);
        }
        validateManualAssignment(old.getType(), targetGroupId, reqVO.getHandlerUserId(), userId, assignAll);
        int updated = workOrderMapper.assignIfPending(old.getId(), CrmWorkOrderStatusEnum.PENDING.getStatus(),
                old.getHandlerUserId(), old.getGroupId(), targetGroupId, reqVO.getHandlerUserId(),
                CrmWorkOrderDispatchModeEnum.REASSIGN.getMode(), LocalDateTime.now());
        if (updated != 1) {
            throw exception(WORK_ORDER_STATUS_TRANSITION_INVALID);
        }
        old.setGroupId(targetGroupId).setHandlerUserId(reqVO.getHandlerUserId())
                .setDispatchMode(CrmWorkOrderDispatchModeEnum.REASSIGN.getMode()).setAssignTime(LocalDateTime.now());
        appendRecord(old, CrmWorkOrderActionTypeEnum.ASSIGN, old.getStatus(), old.getStatus(), userId,
                reqVO.getRemark());
        notificationService.notifyAssigned(old);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void claimWorkOrder(CrmWorkOrderActionReqVO reqVO, Long userId) {
        CrmWorkOrderDO old = workOrderMapper.selectById(reqVO.getId());
        if (old == null) throw exception(WORK_ORDER_NOT_EXISTS);
        if (old.getHandlerUserId() != null || old.getGroupId() == null
                || !groupService.isGroupMember(old.getGroupId(), userId)) {
            throw exception(WORK_ORDER_CLAIM_DENIED);
        }
        int updated = workOrderMapper.claimIfUnassigned(old.getId(), old.getGroupId(), userId, LocalDateTime.now());
        if (updated != 1) throw exception(WORK_ORDER_STATUS_TRANSITION_INVALID);
        old.setHandlerUserId(userId).setDispatchMode(CrmWorkOrderDispatchModeEnum.CLAIM.getMode())
                .setAssignTime(LocalDateTime.now());
        appendRecord(old, CrmWorkOrderActionTypeEnum.CLAIM, old.getStatus(), old.getStatus(), userId, reqVO.getRemark());
        notificationService.notifyAssigned(old);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void startWorkOrder(CrmWorkOrderActionReqVO reqVO, Long userId) {
        CrmWorkOrderDO old = requireParticipant(reqVO.getId(), userId, false);
        requireHandler(old, userId);
        transition(old, workOrderMapper.startIfPending(old.getId(), CrmWorkOrderStatusEnum.PENDING.getStatus(),
                CrmWorkOrderStatusEnum.PROCESSING.getStatus(), LocalDateTime.now()),
                CrmWorkOrderActionTypeEnum.START, userId, reqVO.getRemark());
        if (slaMapper != null) slaMapper.markResponded(old.getId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void returnWorkOrder(CrmWorkOrderReturnReqVO reqVO, Long userId) {
        CrmWorkOrderDO old = requireParticipant(reqVO.getId(), userId, false);
        requireHandler(old, userId);
        transition(old, workOrderMapper.returnIfProcessing(old.getId(), CrmWorkOrderStatusEnum.PROCESSING.getStatus(),
                CrmWorkOrderStatusEnum.RETURNED.getStatus(), reqVO.getReason()),
                CrmWorkOrderActionTypeEnum.RETURN, userId, reqVO.getReason());
        old.setStatus(CrmWorkOrderStatusEnum.RETURNED.getStatus()).setReturnReason(reqVO.getReason());
        notificationService.notifyReturned(old);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void resubmitWorkOrder(CrmWorkOrderActionReqVO reqVO, Long userId) {
        CrmWorkOrderDO old = requireParticipant(reqVO.getId(), userId, false);
        if (!ObjectUtil.equal(old.getCreator(), String.valueOf(userId))) {
            throw exception(WORK_ORDER_CREATOR_ONLY);
        }
        transition(old, workOrderMapper.resubmitIfReturned(old.getId(), CrmWorkOrderStatusEnum.RETURNED.getStatus(),
                CrmWorkOrderStatusEnum.PENDING.getStatus()),
                CrmWorkOrderActionTypeEnum.RESUBMIT, userId, reqVO.getRemark());
        old.setStatus(CrmWorkOrderStatusEnum.PENDING.getStatus());
        notificationService.notifyAssigned(old);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void completeWorkOrder(CrmWorkOrderCompleteReqVO reqVO, Long userId) {
        CrmWorkOrderDO old = requireParticipant(reqVO.getId(), userId, false);
        requireHandler(old, userId);
        if (reqVO.getSolution() == null || reqVO.getSolution().isBlank()) {
            throw exception(WORK_ORDER_SOLUTION_REQUIRED);
        }
        if (reqVO.getSolution().trim().length() < dispatchProperties.getSolutionMinLength()) {
            throw exception(WORK_ORDER_SOLUTION_TOO_SHORT, dispatchProperties.getSolutionMinLength());
        }
        if (Boolean.TRUE.equals(old.getCheckInRequired())
                || governanceProperties != null && governanceProperties.getGeofence().isRequireBeforeComplete()) {
            if (checkInMapper == null || !checkInMapper.exists(old.getId())) {
                throw exception(WORK_ORDER_CHECK_IN_REQUIRED);
            }
        }
        transition(old, workOrderMapper.completeIfProcessing(old.getId(), CrmWorkOrderStatusEnum.PROCESSING.getStatus(),
                CrmWorkOrderStatusEnum.COMPLETED.getStatus(), reqVO.getSolution(), LocalDateTime.now()),
                CrmWorkOrderActionTypeEnum.COMPLETE, userId, null);
        old.setStatus(CrmWorkOrderStatusEnum.COMPLETED.getStatus()).setSolution(reqVO.getSolution());
        if (slaMapper != null) slaMapper.markCompleted(old.getId(), LocalDateTime.now());
        notificationService.notifyCompleted(old, getCcUserIdsMap(List.of(old.getId()))
                .getOrDefault(old.getId(), List.of()));
    }

    @Override
    public Map<Long, List<Long>> getCcUserIdsMap(Collection<Long> workOrderIds) {
        Map<Long, List<Long>> result = new LinkedHashMap<>();
        ccMapper.selectByWorkOrderIds(workOrderIds).forEach(item ->
                result.computeIfAbsent(item.getWorkOrderId(), key -> new ArrayList<>()).add(item.getUserId()));
        return result;
    }

    @Override
    public List<Long> getDispatchCandidateUserIds(Integer type, Long groupId, Long userId, boolean assignAll) {
        if (groupId != null) {
            groupService.validateEnabledGroup(groupId, type);
            if (!assignAll && !groupService.isGroupManager(groupId, userId)) return List.of();
            return groupService.getOrderedMemberUserIds(groupId);
        }
        if (assignAll) {
            return groupService.getGroupList().stream().filter(group -> Integer.valueOf(0).equals(group.getStatus()))
                    .filter(group -> group.getSupportedTypes() != null && group.getSupportedTypes().contains(type))
                    .flatMap(group -> groupService.getOrderedMemberUserIds(group.getId()).stream()).distinct().toList();
        }
        LinkedHashSet<Long> candidates = new LinkedHashSet<>();
        candidates.add(userId);
        adminUserApi.getUserListBySubordinate(userId).forEach(user -> candidates.add(user.getId()));
        return new ArrayList<>(candidates);
    }

    @Override
    public int getOpenWorkOrderCount(Long handlerUserId) {
        return workOrderMapper.countOpenByHandler(handlerUserId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public CrmWorkOrderCheckInDO checkInWorkOrder(CrmWorkOrderCheckInReqVO reqVO, Long userId) {
        CrmWorkOrderDO order = requireParticipant(reqVO.getId(), userId, false);
        requireHandler(order, userId);
        if (governanceProperties == null || !governanceProperties.getGeofence().isEnabled()
                || order.getServiceLatitude() == null || order.getServiceLongitude() == null) {
            throw exception(WORK_ORDER_GEOFENCE_NOT_CONFIGURED);
        }
        if (reqVO.getLatitude() == null || reqVO.getLongitude() == null
                || reqVO.getLatitude().abs().compareTo(BigDecimal.valueOf(90)) > 0
                || reqVO.getLongitude().abs().compareTo(BigDecimal.valueOf(180)) > 0
                || reqVO.getAccuracyMeters() != null
                && reqVO.getAccuracyMeters().compareTo(BigDecimal.valueOf(governanceProperties.getGeofence().getMaxAccuracyMeters())) > 0) {
            throw exception(WORK_ORDER_LOCATION_INVALID);
        }
        BigDecimal distance = distanceMeters(order.getServiceLatitude(), order.getServiceLongitude(),
                reqVO.getLatitude(), reqVO.getLongitude());
        int radius = order.getGeofenceRadiusMeters() == null ? governanceProperties.getGeofence().getDefaultRadiusMeters()
                : order.getGeofenceRadiusMeters();
        if (distance.compareTo(BigDecimal.valueOf(radius)) > 0) throw exception(WORK_ORDER_OUTSIDE_GEOFENCE);
        CrmWorkOrderCheckInDO checkIn = new CrmWorkOrderCheckInDO().setWorkOrderId(order.getId()).setUserId(userId)
                .setLatitude(reqVO.getLatitude()).setLongitude(reqVO.getLongitude())
                .setAccuracyMeters(reqVO.getAccuracyMeters()).setDistanceMeters(distance).setResult(1);
        checkInMapper.insert(checkIn);
        if (Integer.valueOf(CrmWorkOrderStatusEnum.PENDING.getStatus()).equals(order.getStatus())) {
            if (workOrderMapper.startIfPending(order.getId(), 10, 20, LocalDateTime.now()) != 1) {
                throw exception(WORK_ORDER_STATUS_TRANSITION_INVALID);
            }
            appendRecord(order, CrmWorkOrderActionTypeEnum.START, 10, 20, userId, "移动签到自动开始处理");
            order.setStatus(20);
        }
        appendRecord(order, CrmWorkOrderActionTypeEnum.CHECK_IN, order.getStatus(), order.getStatus(), userId,
                reqVO.getRemark());
        return checkIn;
    }

    @Override
    public CrmWorkOrderCheckInDO getLatestCheckIn(Long workOrderId, Long userId, boolean queryAll) {
        requireParticipant(workOrderId, userId, queryAll);
        return checkInMapper == null ? null : checkInMapper.selectLatest(workOrderId);
    }

    @Override
    public CrmWorkOrderSlaDO getWorkOrderSla(Long workOrderId, Long userId, boolean queryAll) {
        requireParticipant(workOrderId, userId, queryAll);
        return slaMapper == null ? null : slaMapper.selectByWorkOrderId(workOrderId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void pauseWorkOrderSla(CrmWorkOrderSlaActionReqVO reqVO, Long userId) {
        CrmWorkOrderDO order = requireParticipant(reqVO.getId(), userId, false);
        requireHandler(order, userId);
        CrmWorkOrderSlaDO sla = requireSla(reqVO.getId());
        if (sla.getPausedAt() != null || slaMapper.pause(sla.getId(), LocalDateTime.now()) != 1) {
            throw exception(WORK_ORDER_SLA_PAUSE_INVALID);
        }
        appendRecord(order, CrmWorkOrderActionTypeEnum.SLA_PAUSE, order.getStatus(), order.getStatus(), userId, reqVO.getRemark());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void resumeWorkOrderSla(CrmWorkOrderSlaActionReqVO reqVO, Long userId) {
        CrmWorkOrderDO order = requireParticipant(reqVO.getId(), userId, false);
        requireHandler(order, userId);
        CrmWorkOrderSlaDO sla = requireSla(reqVO.getId());
        if (sla.getPausedAt() == null) throw exception(WORK_ORDER_SLA_RESUME_INVALID);
        long seconds = java.time.Duration.between(sla.getPausedAt(), LocalDateTime.now()).getSeconds();
        if (slaMapper.resume(sla.getId(), Math.max(0, seconds)) != 1) {
            throw exception(WORK_ORDER_SLA_RESUME_INVALID);
        }
        appendRecord(order, CrmWorkOrderActionTypeEnum.SLA_RESUME, order.getStatus(), order.getStatus(), userId, reqVO.getRemark());
    }

    @Override
    public List<CrmWorkOrderSlaPolicyDO> getSlaPolicies() {
        return slaPolicyMapper == null ? List.of() : slaPolicyMapper.selectEnabled();
    }

    @Override
    public List<CrmWorkOrderHolidayDO> getHolidays() {
        return holidayMapper == null ? List.of() : holidayMapper.selectList();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long saveHoliday(CrmWorkOrderHolidaySaveReqVO reqVO, Long userId) {
        CrmWorkOrderHolidayDO holiday = BeanUtils.toBean(reqVO, CrmWorkOrderHolidayDO.class);
        holiday.setUpdater(String.valueOf(userId));
        if (reqVO.getId() == null) {
            holiday.setCreator(String.valueOf(userId));
            holidayMapper.insert(holiday);
        } else holidayMapper.updateById(holiday);
        return holiday.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteHoliday(Long id, Long userId) {
        holidayMapper.deleteById(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int processDueSla() {
        if (governanceProperties == null || !governanceProperties.getSla().isEnabled() || slaMapper == null) return 0;
        LocalDateTime now = LocalDateTime.now(ZoneId.of(governanceProperties.getSla().getZone()));
        int changed = 0;
        for (CrmWorkOrderSlaDO sla : slaMapper.selectDue(now)) {
            if (sla.getPausedAt() != null) continue;
            if (sla.getResolutionDueTime() != null && !now.isBefore(sla.getResolutionDueTime())) {
                if (slaMapper.markOverdue(sla.getId(), now) == 1) {
                    CrmWorkOrderDO order = workOrderMapper.selectById(sla.getWorkOrderId());
                    if (order != null) appendRecord(order, CrmWorkOrderActionTypeEnum.SLA_ESCALATE,
                            order.getStatus(), order.getStatus(), 0L, "SLA 已逾期");
                    changed++;
                }
            } else if (sla.getEscalationDueTime() != null && !now.isBefore(sla.getEscalationDueTime())) {
                if (slaMapper.markEscalated(sla.getId(), now) == 1) {
                    CrmWorkOrderDO order = workOrderMapper.selectById(sla.getWorkOrderId());
                    if (order != null) appendRecord(order, CrmWorkOrderActionTypeEnum.SLA_ESCALATE,
                            order.getStatus(), order.getStatus(), 0L, "SLA 自动升级");
                    changed++;
                }
            }
        }
        return changed;
    }

    private void initializeSla(CrmWorkOrderDO order) {
        if (governanceProperties == null || !governanceProperties.getSla().isEnabled()
                || slaMapper == null || slaPolicyMapper == null || slaCalculator == null) return;
        CrmWorkOrderSlaPolicyDO policy = slaPolicyMapper.selectEnabled().stream()
                .filter(item -> ObjectUtil.equal(item.getPriority(), order.getPriority())).findFirst().orElse(null);
        if (policy == null) policy = slaPolicyMapper.selectByCode(governanceProperties.getSla().getDefaultPolicyCode());
        if (policy == null) throw exception(WORK_ORDER_SLA_POLICY_INVALID);
        LocalDateTime now = LocalDateTime.now(ZoneId.of(governanceProperties.getSla().getZone()));
        slaMapper.insert(new CrmWorkOrderSlaDO().setWorkOrderId(order.getId()).setPolicyId(policy.getId())
                .setResponseDueTime(slaCalculator.responseDue(now, policy))
                .setEscalationDueTime(slaCalculator.escalationDue(now, policy))
                .setResolutionDueTime(slaCalculator.resolutionDue(now, policy)).setPausedSeconds(0L).setStatus(0));
    }

    private CrmWorkOrderSlaDO requireSla(Long workOrderId) {
        CrmWorkOrderSlaDO sla = slaMapper == null ? null : slaMapper.selectByWorkOrderId(workOrderId);
        if (sla == null) throw exception(WORK_ORDER_SLA_NOT_EXISTS);
        return sla;
    }

    private static BigDecimal distanceMeters(BigDecimal lat1, BigDecimal lon1, BigDecimal lat2, BigDecimal lon2) {
        double earth = 6_371_000d;
        double dLat = Math.toRadians(lat2.doubleValue() - lat1.doubleValue());
        double dLon = Math.toRadians(lon2.doubleValue() - lon1.doubleValue());
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1.doubleValue())) * Math.cos(Math.toRadians(lat2.doubleValue()))
                * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        return BigDecimal.valueOf(earth * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a)))
                .setScale(2, java.math.RoundingMode.HALF_UP);
    }

    private void validateRelations(Long customerId, Integer sourceType, Long sourceId) {
        customerService.validateCustomer(customerId);
        if (CrmWorkOrderSourceTypeEnum.CUSTOMER.getType().equals(sourceType)) {
            if (sourceId != null) throw exception(WORK_ORDER_SOURCE_CUSTOMER_MISMATCH);
            return;
        }
        if (sourceId == null) throw exception(WORK_ORDER_SOURCE_CUSTOMER_MISMATCH);
        Long sourceCustomerId;
        if (CrmWorkOrderSourceTypeEnum.BUSINESS.getType().equals(sourceType)) {
            CrmBusinessDO business = businessService.validateBusiness(sourceId);
            sourceCustomerId = business.getCustomerId();
        } else if (CrmWorkOrderSourceTypeEnum.CONTRACT.getType().equals(sourceType)) {
            CrmContractDO contract = contractService.validateContract(sourceId);
            sourceCustomerId = contract.getCustomerId();
        } else {
            throw exception(WORK_ORDER_SOURCE_CUSTOMER_MISMATCH);
        }
        if (!ObjectUtil.equal(customerId, sourceCustomerId)) {
            throw exception(WORK_ORDER_SOURCE_CUSTOMER_MISMATCH);
        }
    }

    private CrmWorkOrderDO requireParticipant(Long id, Long userId, boolean queryAll) {
        CrmWorkOrderDO workOrder = workOrderMapper.selectById(id);
        if (workOrder == null) throw exception(WORK_ORDER_NOT_EXISTS);
        boolean groupAccess = groupService.isGroupManager(workOrder.getGroupId(), userId)
                || workOrder.getHandlerUserId() == null && workOrder.getGroupId() != null
                && groupService.isGroupMember(workOrder.getGroupId(), userId);
        if (!queryAll && !ObjectUtil.equal(workOrder.getCreator(), String.valueOf(userId))
                && !ObjectUtil.equal(workOrder.getHandlerUserId(), userId)
                && !groupAccess && !ccMapper.exists(id, userId)) {
            throw exception(WORK_ORDER_QUERY_DENIED);
        }
        return workOrder;
    }

    private void requireHandler(CrmWorkOrderDO workOrder, Long userId) {
        if (workOrder.getHandlerUserId() == null) throw exception(WORK_ORDER_UNASSIGNED);
        if (!ObjectUtil.equal(workOrder.getHandlerUserId(), userId)) throw exception(WORK_ORDER_HANDLER_ONLY);
    }

    private void transition(CrmWorkOrderDO old, int updated, CrmWorkOrderActionTypeEnum action,
                            Long userId, String remark) {
        if (updated != 1) throw exception(WORK_ORDER_STATUS_TRANSITION_INVALID);
        Integer toStatus = switch (action) {
            case START -> CrmWorkOrderStatusEnum.PROCESSING.getStatus();
            case RETURN -> CrmWorkOrderStatusEnum.RETURNED.getStatus();
            case RESUBMIT -> CrmWorkOrderStatusEnum.PENDING.getStatus();
            case COMPLETE -> CrmWorkOrderStatusEnum.COMPLETED.getStatus();
            default -> old.getStatus();
        };
        appendRecord(old, action, old.getStatus(), toStatus, userId, remark);
    }

    private void appendRecord(CrmWorkOrderDO order, CrmWorkOrderActionTypeEnum action, Integer from, Integer to,
                              Long userId, String remark) {
        CrmWorkOrderRecordDO record = new CrmWorkOrderRecordDO().setWorkOrderId(order.getId())
                .setActionType(action.getType()).setFromStatus(from).setToStatus(to)
                .setOperatorUserId(userId).setHandlerUserId(order.getHandlerUserId()).setRemark(remark);
        recordMapper.insert(record);
    }

    private DispatchDecision resolveCreateDispatch(Integer type, Long requestedGroupId, Long requestedHandlerId,
                                                    Long userId, boolean canAssign, boolean assignAll) {
        if (!dispatchProperties.isEnabled()) {
            if (requestedHandlerId == null) throw exception(WORK_ORDER_HANDLER_REQUIRED);
            adminUserApi.validateUser(requestedHandlerId);
            return new DispatchDecision(requestedGroupId, requestedHandlerId,
                    CrmWorkOrderDispatchModeEnum.MANUAL.getMode());
        }
        if (requestedHandlerId != null) {
            if (!canAssign) throw exception(WORK_ORDER_MANUAL_ASSIGN_DENIED);
            validateManualAssignment(type, requestedGroupId, requestedHandlerId, userId, assignAll);
            return new DispatchDecision(requestedGroupId, requestedHandlerId,
                    CrmWorkOrderDispatchModeEnum.MANUAL.getMode());
        }
        if (dispatchProperties.isAutoAssignOnCreate()) {
            AutoCandidate auto = selectAutoCandidate(type, requestedGroupId);
            if (auto != null) {
                return new DispatchDecision(auto.groupId(), auto.userId(), CrmWorkOrderDispatchModeEnum.AUTO.getMode());
            }
        }
        if (dispatchProperties.getFallbackMode() == CrmWorkOrderDispatchProperties.FallbackMode.REQUIRE_HANDLER) {
            throw exception(WORK_ORDER_HANDLER_REQUIRED);
        }
        if (requestedGroupId != null) groupService.validateEnabledGroup(requestedGroupId, type);
        return new DispatchDecision(requestedGroupId, null, CrmWorkOrderDispatchModeEnum.UNASSIGNED.getMode());
    }

    private AutoCandidate selectAutoCandidate(Integer type, Long requestedGroupId) {
        List<CrmWorkOrderGroupDO> groups = requestedGroupId == null
                ? groupService.getGroupList().stream().filter(group -> Integer.valueOf(0).equals(group.getStatus()))
                .filter(group -> group.getSupportedTypes() != null && group.getSupportedTypes().contains(type)).toList()
                : List.of(groupService.validateEnabledGroup(requestedGroupId, type));
        return groups.stream().flatMap(group -> groupService.getOrderedMemberUserIds(group.getId()).stream()
                        .map(userId -> new AutoCandidate(group.getId(), userId, getOpenWorkOrderCount(userId), group.getSort())))
                .min(Comparator.comparingInt(AutoCandidate::openCount).thenComparingInt(AutoCandidate::groupSort)
                        .thenComparing(AutoCandidate::groupId).thenComparing(AutoCandidate::userId))
                .orElse(null);
    }

    private void validateManualAssignment(Integer type, Long groupId, Long handlerUserId, Long userId,
                                          boolean assignAll) {
        adminUserApi.validateUser(handlerUserId);
        if (groupId != null && !assignAll && !groupService.isGroupManager(groupId, userId)) {
            throw exception(WORK_ORDER_ASSIGN_DENIED);
        }
        List<Long> candidates = getDispatchCandidateUserIds(type, groupId, userId, assignAll);
        if (!candidates.contains(handlerUserId)) throw exception(WORK_ORDER_HANDLER_NOT_ELIGIBLE);
    }

    private void validateDescription(String description) {
        if (description == null || description.trim().length() < dispatchProperties.getDescriptionMinLength()) {
            throw exception(WORK_ORDER_DESCRIPTION_TOO_SHORT, dispatchProperties.getDescriptionMinLength());
        }
    }

    private void validateServiceLocation(CrmWorkOrderSaveReqVO reqVO) {
        boolean hasLatitude = reqVO.getServiceLatitude() != null;
        boolean hasLongitude = reqVO.getServiceLongitude() != null;
        if (hasLatitude != hasLongitude || Boolean.TRUE.equals(reqVO.getCheckInRequired()) && !hasLatitude) {
            throw exception(WORK_ORDER_GEOFENCE_NOT_CONFIGURED);
        }
    }

    private void validateCcUsers(List<Long> ccUserIds) {
        Set<Long> users = distinctIds(ccUserIds);
        if (users.size() > dispatchProperties.getMaxCcUsers()) {
            throw exception(WORK_ORDER_CC_LIMIT_EXCEEDED, dispatchProperties.getMaxCcUsers());
        }
        if (!users.isEmpty()) adminUserApi.validateUserList(users);
    }

    private void replaceCcUsers(Long workOrderId, List<Long> ccUserIds) {
        ccMapper.deleteByWorkOrderId(workOrderId);
        for (Long userId : distinctIds(ccUserIds)) {
            ccMapper.insert(new CrmWorkOrderCcDO().setWorkOrderId(workOrderId).setUserId(userId));
        }
    }

    private static LinkedHashSet<Long> distinctIds(Collection<Long> ids) {
        if (ids == null) return new LinkedHashSet<>();
        return ids.stream().filter(java.util.Objects::nonNull)
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    private record DispatchDecision(Long groupId, Long handlerUserId, Integer mode) {}
    private record AutoCandidate(Long groupId, Long userId, int openCount, int groupSort) {}
}
