package com.meession.etm.module.crm.service.marketing;

import com.meession.etm.framework.common.pojo.PageResult;
import com.meession.etm.module.crm.controller.admin.marketing.vo.*;
import com.meession.etm.module.crm.dal.dataobject.marketing.CrmCustomerCarePlanDO;
import com.meession.etm.module.crm.dal.dataobject.marketing.CrmCustomerCareRecordDO;

public interface CrmCustomerCareService {
    Long savePlan(CrmCustomerCarePlanSaveReqVO request);
    PageResult<CrmCustomerCarePlanDO> getPlanPage(CrmCustomerCarePlanPageReqVO request);
    PageResult<CrmCustomerCareRecordDO> getRecordPage(CrmCustomerCareRecordPageReqVO request);
    int generateAndSendToday();
}
