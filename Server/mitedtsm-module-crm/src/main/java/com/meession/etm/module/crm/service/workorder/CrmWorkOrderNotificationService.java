package com.meession.etm.module.crm.service.workorder;

import com.meession.etm.module.crm.dal.dataobject.workorder.CrmWorkOrderDO;

public interface CrmWorkOrderNotificationService {
    void notifyAssigned(CrmWorkOrderDO workOrder);
    void notifyReturned(CrmWorkOrderDO workOrder);
    void notifyCompleted(CrmWorkOrderDO workOrder);
}
