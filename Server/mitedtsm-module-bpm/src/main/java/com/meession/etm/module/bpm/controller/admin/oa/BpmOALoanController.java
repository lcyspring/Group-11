package com.meession.etm.module.bpm.controller.admin.oa;

import com.meession.etm.framework.common.pojo.CommonResult;
import com.meession.etm.framework.common.pojo.PageResult;
import com.meession.etm.framework.common.util.object.BeanUtils;
import com.meession.etm.module.bpm.controller.admin.oa.vo.BpmOALoanCreateReqVO;
import com.meession.etm.module.bpm.controller.admin.oa.vo.BpmOALoanPageReqVO;
import com.meession.etm.module.bpm.controller.admin.oa.vo.BpmOALoanRepaymentCreateReqVO;
import com.meession.etm.module.bpm.controller.admin.oa.vo.BpmOALoanRespVO;
import com.meession.etm.module.bpm.controller.admin.oa.vo.BpmOALoanLimitRespVO;
import com.meession.etm.module.bpm.dal.dataobject.oa.BpmOALoanDO;
import com.meession.etm.module.bpm.dal.dataobject.oa.BpmOALoanRepaymentDO;
import com.meession.etm.module.bpm.service.oa.BpmOALoanService;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

import static com.meession.etm.framework.common.pojo.CommonResult.success;
import static com.meession.etm.framework.security.core.util.SecurityFrameworkUtils.getLoginUserId;

@RestController
@RequestMapping("/bpm/oa/loan")
public class BpmOALoanController {
    @Resource private BpmOALoanService loanService;

    @PostMapping("/create") @PreAuthorize("@ss.hasPermission('bpm:oa-loan:create')")
    public CommonResult<Long> create(@Valid @RequestBody BpmOALoanCreateReqVO request) {
        return success(loanService.createLoan(getLoginUserId(), request));
    }
    @GetMapping("/get") @PreAuthorize("@ss.hasPermission('bpm:oa-loan:query')")
    public CommonResult<BpmOALoanRespVO> get(@RequestParam Long id) {
        return success(BeanUtils.toBean(loanService.getLoan(getLoginUserId(), id), BpmOALoanRespVO.class));
    }
    @GetMapping("/get-by-process-instance") @PreAuthorize("@ss.hasPermission('bpm:oa-loan:query')")
    public CommonResult<BpmOALoanRespVO> getByProcessInstance(@RequestParam String processInstanceId) {
        return success(BeanUtils.toBean(loanService.getLoanByProcessInstanceId(getLoginUserId(), processInstanceId),
                BpmOALoanRespVO.class));
    }
    @GetMapping("/page") @PreAuthorize("@ss.hasPermission('bpm:oa-loan:query')")
    public CommonResult<PageResult<BpmOALoanRespVO>> page(@Valid BpmOALoanPageReqVO request) {
        PageResult<BpmOALoanDO> page = loanService.getLoanPage(getLoginUserId(), request);
        return success(BeanUtils.toBean(page, BpmOALoanRespVO.class));
    }
    @PostMapping("/repayment/create") @PreAuthorize("@ss.hasPermission('bpm:oa-loan:update')")
    public CommonResult<Long> createRepayment(@Valid @RequestBody BpmOALoanRepaymentCreateReqVO request) {
        return success(loanService.createRepayment(getLoginUserId(), request));
    }
    @GetMapping("/repayment/list") @PreAuthorize("@ss.hasPermission('bpm:oa-loan:query')")
    public CommonResult<List<BpmOALoanRepaymentDO>> repayments(@RequestParam Long loanId) {
        return success(loanService.getRepayments(getLoginUserId(), loanId));
    }
    @GetMapping("/my-limit") @PreAuthorize("@ss.hasPermission('bpm:oa-loan:query')")
    public CommonResult<BpmOALoanLimitRespVO> myLimit() {
        return success(loanService.getMyLimit(getLoginUserId()));
    }
}
