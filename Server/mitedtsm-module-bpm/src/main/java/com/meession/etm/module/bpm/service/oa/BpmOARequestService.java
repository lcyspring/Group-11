package com.meession.etm.module.bpm.service.oa;

import com.meession.etm.framework.common.pojo.PageResult;
import com.meession.etm.module.bpm.controller.admin.oa.vo.BpmOARequestCreateReqVO;
import com.meession.etm.module.bpm.controller.admin.oa.vo.BpmOARequestPageReqVO;
import com.meession.etm.module.bpm.dal.dataobject.oa.BpmOARequestDO;

public interface BpmOARequestService {

    Long createRequest(Long userId, BpmOARequestCreateReqVO createReqVO);

    void updateRequestStatus(Long id, Integer status);

    BpmOARequestDO getRequest(Long id);

    PageResult<BpmOARequestDO> getRequestPage(Long userId, BpmOARequestPageReqVO pageReqVO);

}