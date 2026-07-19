package com.meession.etm.module.crm.service.workorder;

import com.meession.etm.module.crm.dal.dataobject.workorder.CrmWorkOrderDO;

import java.util.Collection;

public interface CrmWorkOrderNotificationService {
    void notifyAssigned(CrmWorkOrderDO workOrder);
    void notifyCopied(CrmWorkOrderDO workOrder, Collection<Long> userIds);
    void notifyReturned(CrmWorkOrderDO workOrder);
    void notifyCompleted(CrmWorkOrderDO workOrder, Collection<Long> ccUserIds);
}
