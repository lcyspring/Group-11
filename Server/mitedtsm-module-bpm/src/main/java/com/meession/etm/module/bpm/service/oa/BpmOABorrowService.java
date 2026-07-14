package com.meession.etm.module.bpm.service.oa;

import com.meession.etm.module.bpm.controller.admin.oa.vo.BpmOABorrowCreateReqVO;
import com.meession.etm.module.bpm.controller.admin.oa.vo.BpmOABorrowPageReqVO;
import com.meession.etm.module.bpm.dal.dataobject.oa.BpmOABorrowDO;
import com.meession.etm.framework.common.pojo.PageResult;

public interface BpmOABorrowService {

    Long createBorrow(Long userId, BpmOABorrowCreateReqVO createReqVO);

    void updateBorrowStatus(Long id, Integer status);

    BpmOABorrowDO getBorrow(Long id);

    PageResult<BpmOABorrowDO> getBorrowPage(Long userId, BpmOABorrowPageReqVO pageReqVO);

}