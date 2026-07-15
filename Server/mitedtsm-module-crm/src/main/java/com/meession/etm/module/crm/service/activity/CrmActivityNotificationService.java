package com.meession.etm.module.crm.service.activity;

import com.meession.etm.module.crm.dal.dataobject.activity.CrmTaskDO;

public interface CrmActivityNotificationService {
    void notifyAssigned(CrmTaskDO task);
    void notifyFinished(CrmTaskDO task);
}
