package com.meession.etm.module.crm.service.workorder;

import com.meession.etm.module.crm.controller.admin.workorder.vo.CrmWorkOrderGroupSaveReqVO;
import com.meession.etm.module.crm.dal.dataobject.workorder.CrmWorkOrderGroupDO;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

public interface CrmWorkOrderGroupService {
    List<CrmWorkOrderGroupDO> getGroupList();
    Map<Long, CrmWorkOrderGroupDO> getGroupMap(Collection<Long> ids);
    Map<Long, List<Long>> getMemberUserIdsMap(Collection<Long> groupIds);
    Long saveGroup(CrmWorkOrderGroupSaveReqVO request);
    void deleteGroup(Long id);
    CrmWorkOrderGroupDO validateEnabledGroup(Long groupId, Integer workOrderType);
    boolean isGroupMember(Long groupId, Long userId);
    boolean isGroupManager(Long groupId, Long userId);
    Set<Long> getMemberGroupIds(Long userId);
    Set<Long> getManagedGroupIds(Long userId);
    List<Long> getOrderedMemberUserIds(Long groupId);
}
