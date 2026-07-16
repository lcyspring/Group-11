package com.meession.etm.module.crm.service.marketing;

import com.meession.etm.framework.common.pojo.PageResult;
import com.meession.etm.module.crm.controller.admin.marketing.vo.*;
import com.meession.etm.module.crm.dal.dataobject.marketing.CrmCustomerCarePlanDO;

public interface CrmCustomerCareService {
    Long savePlan(CrmCustomerCarePlanSaveReqVO request);
    CrmCustomerCarePlanDO getPlan(Long id);
    void updatePlanStatus(CrmCustomerCarePlanStatusReqVO request);
    void deletePlan(Long id);
    PageResult<CrmCustomerCarePlanDO> getPlanPage(CrmCustomerCarePlanPageReqVO request);
    PageResult<CrmCustomerCareRecordRespVO> getRecordPage(CrmCustomerCareRecordPageReqVO request, Long userId);
    PageResult<CrmCustomerBirthdayRespVO> getBirthdayPage(CrmCustomerBirthdayPageReqVO request, Long userId);
    int generateAndSendToday();
}
