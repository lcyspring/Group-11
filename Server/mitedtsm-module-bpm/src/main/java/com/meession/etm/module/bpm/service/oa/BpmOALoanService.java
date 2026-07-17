package com.meession.etm.module.bpm.service.oa;

import com.meession.etm.framework.common.pojo.PageResult;
import com.meession.etm.module.bpm.controller.admin.oa.vo.BpmOALoanCreateReqVO;
import com.meession.etm.module.bpm.controller.admin.oa.vo.BpmOALoanPageReqVO;
import com.meession.etm.module.bpm.controller.admin.oa.vo.BpmOALoanRepaymentCreateReqVO;
import com.meession.etm.module.bpm.controller.admin.oa.vo.BpmOALoanLimitRespVO;
import com.meession.etm.module.bpm.dal.dataobject.oa.BpmOALoanDO;
import com.meession.etm.module.bpm.dal.dataobject.oa.BpmOALoanRepaymentDO;
import jakarta.validation.Valid;

import java.util.List;

public interface BpmOALoanService {
    Long createLoan(Long userId, @Valid BpmOALoanCreateReqVO request);
    BpmOALoanDO getLoan(Long userId, Long id);
    PageResult<BpmOALoanDO> getLoanPage(Long userId, BpmOALoanPageReqVO request);
    void updateLoanStatus(Long id, Integer status);
    Long createRepayment(Long userId, @Valid BpmOALoanRepaymentCreateReqVO request);
    List<BpmOALoanRepaymentDO> getRepayments(Long userId, Long loanId);
    BpmOALoanLimitRespVO getMyLimit(Long userId);
}
