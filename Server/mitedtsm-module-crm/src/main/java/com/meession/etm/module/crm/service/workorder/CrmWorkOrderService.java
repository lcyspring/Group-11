package com.meession.etm.module.crm.service.workorder;

import com.meession.etm.framework.common.pojo.PageResult;
import com.meession.etm.module.crm.controller.admin.workorder.vo.*;
import com.meession.etm.module.crm.dal.dataobject.workorder.CrmWorkOrderDO;
import com.meession.etm.module.crm.dal.dataobject.workorder.CrmWorkOrderRecordDO;

import java.util.List;
import java.util.Collection;
import java.util.Map;

public interface CrmWorkOrderService {
    Long createWorkOrder(CrmWorkOrderSaveReqVO reqVO, Long userId, boolean canAssign, boolean assignAll);
    void updateWorkOrder(CrmWorkOrderSaveReqVO reqVO, Long userId);
    void deleteWorkOrder(Long id, Long userId);
    CrmWorkOrderDO getWorkOrder(Long id, Long userId, boolean queryAll);
    PageResult<CrmWorkOrderDO> getWorkOrderPage(CrmWorkOrderPageReqVO reqVO, Long userId, boolean queryAll);
    List<CrmWorkOrderRecordDO> getWorkOrderRecords(Long id, Long userId, boolean queryAll);
    void assignWorkOrder(CrmWorkOrderAssignReqVO reqVO, Long userId, boolean assignAll);
    void claimWorkOrder(CrmWorkOrderActionReqVO reqVO, Long userId);
    void startWorkOrder(CrmWorkOrderActionReqVO reqVO, Long userId);
    void returnWorkOrder(CrmWorkOrderReturnReqVO reqVO, Long userId);
    void resubmitWorkOrder(CrmWorkOrderActionReqVO reqVO, Long userId);
    void completeWorkOrder(CrmWorkOrderCompleteReqVO reqVO, Long userId);
    Map<Long, List<Long>> getCcUserIdsMap(Collection<Long> workOrderIds);
    List<Long> getDispatchCandidateUserIds(Integer type, Long groupId, Long userId, boolean assignAll);
    int getOpenWorkOrderCount(Long handlerUserId);
}
