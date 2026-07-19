package com.meession.etm.module.bpm.service.oa;

import com.meession.etm.framework.common.pojo.PageResult;
import com.meession.etm.module.bpm.controller.admin.oa.vo.BpmOATripCreateReqVO;
import com.meession.etm.module.bpm.controller.admin.oa.vo.BpmOATripPageReqVO;
import com.meession.etm.module.bpm.dal.dataobject.oa.BpmOATripDO;
import jakarta.validation.Valid;

import java.time.LocalDate;
import java.util.List;

public interface BpmOATripService {
    Long createTrip(Long userId, @Valid BpmOATripCreateReqVO request);
    BpmOATripDO getTrip(Long userId, Long id);
    PageResult<BpmOATripDO> getTripPage(Long userId, BpmOATripPageReqVO request);
    void updateTripStatus(Long id, Integer status);
    List<BpmOATripDO> getReimbursableTrips(Long userId);
    BpmOATripDO validateReimbursableTrip(Long userId, Long id, LocalDate expenseStartDate, LocalDate expenseEndDate);
}
