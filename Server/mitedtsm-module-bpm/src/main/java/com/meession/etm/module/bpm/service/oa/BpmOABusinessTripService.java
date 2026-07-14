package com.meession.etm.module.bpm.service.oa;

import com.meession.etm.module.bpm.controller.admin.oa.vo.BpmOABusinessTripCreateReqVO;
import com.meession.etm.module.bpm.controller.admin.oa.vo.BpmOABusinessTripPageReqVO;
import com.meession.etm.module.bpm.dal.dataobject.oa.BpmOABusinessTripDO;
import com.meession.etm.framework.common.pojo.PageResult;

public interface BpmOABusinessTripService {

    Long createBusinessTrip(Long userId, BpmOABusinessTripCreateReqVO createReqVO);

    void updateBusinessTripStatus(Long id, Integer status);

    BpmOABusinessTripDO getBusinessTrip(Long id);

    PageResult<BpmOABusinessTripDO> getBusinessTripPage(Long userId, BpmOABusinessTripPageReqVO pageReqVO);

}