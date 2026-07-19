package com.meession.etm.module.crm.service.visit;

import com.meession.etm.framework.common.pojo.PageResult;
import com.meession.etm.module.crm.controller.admin.visit.vo.CrmCustomerVisitCreateReqVO;
import com.meession.etm.module.crm.controller.admin.visit.vo.CrmCustomerVisitPageReqVO;
import com.meession.etm.module.crm.controller.admin.visit.vo.CrmCustomerVisitResultReqVO;
import com.meession.etm.module.crm.dal.dataobject.visit.CrmCustomerVisitDO;
import jakarta.validation.Valid;

public interface CrmCustomerVisitService {
    Long createVisit(Long userId, @Valid CrmCustomerVisitCreateReqVO request);
    CrmCustomerVisitDO getVisit(Long userId, Long id);
    PageResult<CrmCustomerVisitDO> getVisitPage(Long userId, CrmCustomerVisitPageReqVO request);
    void updateAuditStatus(Long id, String processInstanceId, Integer status);
    Long recordResult(Long userId, @Valid CrmCustomerVisitResultReqVO request);
}
