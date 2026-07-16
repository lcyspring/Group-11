package com.meession.etm.module.crm.service.workorder;

import com.meession.etm.framework.common.util.object.BeanUtils;
import com.meession.etm.module.crm.controller.admin.workorder.vo.CrmWorkOrderGroupSaveReqVO;
import com.meession.etm.module.crm.dal.dataobject.workorder.CrmWorkOrderDO;
import com.meession.etm.module.crm.dal.dataobject.workorder.CrmWorkOrderGroupDO;
import com.meession.etm.module.crm.dal.dataobject.workorder.CrmWorkOrderGroupMemberDO;
import com.meession.etm.module.crm.dal.mysql.workorder.CrmWorkOrderGroupMapper;
import com.meession.etm.module.crm.dal.mysql.workorder.CrmWorkOrderGroupMemberMapper;
import com.meession.etm.module.crm.dal.mysql.workorder.CrmWorkOrderMapper;
import com.meession.etm.module.crm.enums.workorder.CrmWorkOrderTypeEnum;
import com.meession.etm.module.system.api.user.AdminUserApi;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static com.meession.etm.framework.common.exception.util.ServiceExceptionUtil.exception;
import static com.meession.etm.module.crm.enums.ErrorCodeConstants.*;

@Service
public class CrmWorkOrderGroupServiceImpl implements CrmWorkOrderGroupService {

    @Resource private CrmWorkOrderGroupMapper groupMapper;
    @Resource private CrmWorkOrderGroupMemberMapper memberMapper;
    @Resource private CrmWorkOrderMapper workOrderMapper;
    @Resource private AdminUserApi adminUserApi;

    @Override
    public List<CrmWorkOrderGroupDO> getGroupList() {
        return groupMapper.selectListOrdered();
    }

    @Override
    public Map<Long, CrmWorkOrderGroupDO> getGroupMap(Collection<Long> ids) {
        if (ids == null || ids.isEmpty()) return Map.of();
        return groupMapper.selectBatchIds(ids).stream().collect(Collectors.toMap(CrmWorkOrderGroupDO::getId, item -> item));
    }

    @Override
    public Map<Long, List<Long>> getMemberUserIdsMap(Collection<Long> groupIds) {
        Map<Long, List<Long>> result = new LinkedHashMap<>();
        memberMapper.selectByGroupIds(groupIds).forEach(member ->
                result.computeIfAbsent(member.getGroupId(), key -> new ArrayList<>()).add(member.getUserId()));
        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long saveGroup(CrmWorkOrderGroupSaveReqVO request) {
        validateRequest(request);
        CrmWorkOrderGroupDO sameCode = groupMapper.selectByCode(request.getCode());
        if (sameCode != null && !sameCode.getId().equals(request.getId())) {
            throw exception(WORK_ORDER_GROUP_CODE_EXISTS);
        }
        CrmWorkOrderGroupDO group = BeanUtils.toBean(request, CrmWorkOrderGroupDO.class);
        if (request.getId() == null) {
            groupMapper.insert(group);
        } else {
            if (groupMapper.selectById(request.getId()) == null) throw exception(WORK_ORDER_GROUP_NOT_EXISTS);
            groupMapper.updateById(group);
            memberMapper.physicalDeleteByGroupId(group.getId());
        }
        int sort = 1;
        for (Long userId : new LinkedHashSet<>(request.getMemberUserIds())) {
            memberMapper.insert(new CrmWorkOrderGroupMemberDO().setGroupId(group.getId()).setUserId(userId).setSort(sort++));
        }
        return group.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteGroup(Long id) {
        if (groupMapper.selectById(id) == null) throw exception(WORK_ORDER_GROUP_NOT_EXISTS);
        if (workOrderMapper.selectCount(CrmWorkOrderDO::getGroupId, id) > 0) throw exception(WORK_ORDER_GROUP_IN_USE);
        memberMapper.deleteByGroupId(id);
        groupMapper.deleteById(id);
    }

    @Override
    public CrmWorkOrderGroupDO validateEnabledGroup(Long groupId, Integer workOrderType) {
        CrmWorkOrderGroupDO group = groupMapper.selectById(groupId);
        if (group == null) throw exception(WORK_ORDER_GROUP_NOT_EXISTS);
        if (!Integer.valueOf(0).equals(group.getStatus())) throw exception(WORK_ORDER_GROUP_DISABLED);
        if (group.getSupportedTypes() == null || !group.getSupportedTypes().contains(workOrderType)) {
            throw exception(WORK_ORDER_GROUP_TYPE_UNSUPPORTED);
        }
        return group;
    }

    @Override
    public boolean isGroupMember(Long groupId, Long userId) {
        return memberMapper.selectByGroupIds(List.of(groupId)).stream().anyMatch(item -> item.getUserId().equals(userId));
    }

    @Override
    public boolean isGroupManager(Long groupId, Long userId) {
        CrmWorkOrderGroupDO group = groupId == null ? null : groupMapper.selectById(groupId);
        return group != null && userId.equals(group.getManagerUserId());
    }

    @Override
    public Set<Long> getMemberGroupIds(Long userId) {
        return memberMapper.selectByUserId(userId).stream().map(CrmWorkOrderGroupMemberDO::getGroupId)
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    @Override
    public Set<Long> getManagedGroupIds(Long userId) {
        return groupMapper.selectList(CrmWorkOrderGroupDO::getManagerUserId, userId).stream()
                .map(CrmWorkOrderGroupDO::getId).collect(Collectors.toCollection(LinkedHashSet::new));
    }

    @Override
    public List<Long> getOrderedMemberUserIds(Long groupId) {
        return memberMapper.selectByGroupIds(List.of(groupId)).stream().map(CrmWorkOrderGroupMemberDO::getUserId).toList();
    }

    private void validateRequest(CrmWorkOrderGroupSaveReqVO request) {
        Set<Integer> supported = new LinkedHashSet<>(request.getSupportedTypes());
        if (supported.size() != request.getSupportedTypes().size()
                || java.util.Arrays.stream(CrmWorkOrderTypeEnum.values()).map(CrmWorkOrderTypeEnum::getType)
                .noneMatch(supported::contains)
                || supported.stream().anyMatch(type -> java.util.Arrays.stream(CrmWorkOrderTypeEnum.values())
                .noneMatch(item -> item.getType().equals(type)))) {
            throw exception(WORK_ORDER_GROUP_TYPE_UNSUPPORTED);
        }
        Set<Long> members = new LinkedHashSet<>(request.getMemberUserIds());
        if (members.isEmpty()) throw exception(WORK_ORDER_GROUP_MEMBER_REQUIRED);
        if (!members.contains(request.getManagerUserId())) throw exception(WORK_ORDER_GROUP_MANAGER_NOT_MEMBER);
        adminUserApi.validateUserList(members);
    }
}
