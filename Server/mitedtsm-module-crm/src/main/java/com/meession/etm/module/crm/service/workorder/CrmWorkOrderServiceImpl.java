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
import com.meession.etm.module.crm.dal.redis.no.CrmNoRedisDAO;
import com.meession.etm.module.crm.enums.workorder.*;
import com.meession.etm.module.crm.framework.workorder.CrmWorkOrderDispatchProperties;
import com.meession.etm.module.crm.service.business.CrmBusinessService;
import com.meession.etm.module.crm.service.contract.CrmContractService;
import com.meession.etm.module.crm.service.customer.CrmCustomerService;
import com.meession.etm.module.system.api.user.AdminUserApi;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.time.LocalDateTime;
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

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createWorkOrder(CrmWorkOrderSaveReqVO reqVO, Long userId, boolean canAssign, boolean assignAll) {
        validateRelations(reqVO.getCustomerId(), reqVO.getSourceType(), reqVO.getSourceId());
        validateDescription(reqVO.getDescription());
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
        workOrder.setCreator(String.valueOf(userId));
        workOrderMapper.insert(workOrder);
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
        if (ObjectUtil.equal(old.getHandlerUserId(), reqVO.getHandlerUserId())) {
            throw exception(WORK_ORDER_HANDLER_UNCHANGED);
        }
        Long targetGroupId = reqVO.getGroupId() == null ? old.getGroupId() : reqVO.getGroupId();
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
        transition(old, workOrderMapper.completeIfProcessing(old.getId(), CrmWorkOrderStatusEnum.PROCESSING.getStatus(),
                CrmWorkOrderStatusEnum.COMPLETED.getStatus(), reqVO.getSolution(), LocalDateTime.now()),
                CrmWorkOrderActionTypeEnum.COMPLETE, userId, null);
        old.setStatus(CrmWorkOrderStatusEnum.COMPLETED.getStatus()).setSolution(reqVO.getSolution());
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
