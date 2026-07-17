package com.meession.etm.module.bpm.service.oa;

import com.meession.etm.framework.common.pojo.PageResult;
import com.meession.etm.framework.common.util.object.BeanUtils;
import com.meession.etm.module.bpm.api.task.BpmProcessInstanceApi;
import com.meession.etm.module.bpm.api.task.dto.BpmProcessInstanceCreateReqDTO;
import com.meession.etm.module.bpm.controller.admin.oa.vo.BpmOALoanCreateReqVO;
import com.meession.etm.module.bpm.controller.admin.oa.vo.BpmOALoanPageReqVO;
import com.meession.etm.module.bpm.controller.admin.oa.vo.BpmOALoanRepaymentCreateReqVO;
import com.meession.etm.module.bpm.controller.admin.oa.vo.BpmOALoanLimitRespVO;
import com.meession.etm.module.bpm.dal.dataobject.oa.BpmOALoanDO;
import com.meession.etm.module.bpm.dal.dataobject.oa.BpmOALoanRepaymentDO;
import com.meession.etm.module.bpm.dal.mysql.oa.BpmOALoanMapper;
import com.meession.etm.module.bpm.dal.mysql.oa.BpmOALoanRepaymentMapper;
import com.meession.etm.module.bpm.enums.task.BpmTaskStatusEnum;
import com.meession.etm.module.bpm.framework.oa.BpmOALoanProperties;
import com.meession.etm.module.system.api.dept.PostApi;
import com.meession.etm.module.system.api.dept.dto.PostRespDTO;
import com.meession.etm.module.system.api.user.AdminUserApi;
import com.meession.etm.module.system.api.user.dto.AdminUserRespDTO;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import static com.meession.etm.framework.common.exception.util.ServiceExceptionUtil.exception;
import static com.meession.etm.module.bpm.enums.ErrorCodeConstants.*;

@Service
@Validated
public class BpmOALoanServiceImpl implements BpmOALoanService {
    public static final String PROCESS_KEY = "oa_loan";
    private static final int REPAYMENT_NONE = 0;
    private static final int REPAYMENT_OUTSTANDING = 1;
    private static final int REPAYMENT_PAID = 2;

    @Resource private BpmOALoanMapper loanMapper;
    @Resource private BpmOALoanRepaymentMapper repaymentMapper;
    @Resource private BpmProcessInstanceApi processInstanceApi;
    @Resource private AdminUserApi adminUserApi;
    @Resource private PostApi postApi;
    @Resource private BpmOATripService tripService;
    @Resource private BpmOALoanProperties properties;

    @PostConstruct
    void validateConfiguration() {
        if (properties.getEmployeeLimit().signum() <= 0
                || properties.getManagerLimit().compareTo(properties.getEmployeeLimit()) < 0
                || properties.getDirectorLimit().compareTo(properties.getManagerLimit()) < 0) {
            throw new IllegalStateException("OA loan limits must satisfy 0 < employee <= manager <= director");
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createLoan(Long userId, BpmOALoanCreateReqVO request) {
        List<BpmOALoanDO> outstanding = loanMapper.selectOutstandingForUpdate(userId);
        BigDecimal outstandingAmount = outstanding.stream().map(BpmOALoanDO::getOutstandingAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        if (outstandingAmount.signum() > 0) {
            throw exception(OA_LOAN_OUTSTANDING_EXISTS, outstandingAmount.stripTrailingZeros().toPlainString());
        }
        if (request.getTripId() != null) {
            tripService.getTrip(userId, request.getTripId());
        }
        EmployeeLimit employeeLimit = resolveEmployeeLimit(userId);
        boolean escalated = request.getAmount().compareTo(employeeLimit.limit()) > 0;
        BpmOALoanDO loan = BeanUtils.toBean(request, BpmOALoanDO.class).setUserId(userId)
                .setEmployeeLevel(employeeLimit.level()).setApprovalLimit(employeeLimit.limit())
                .setEscalatedApproval(escalated).setOutstandingAmount(BigDecimal.ZERO)
                .setRepaymentStatus(REPAYMENT_NONE).setStatus(BpmTaskStatusEnum.RUNNING.getStatus());
        loanMapper.insert(loan);
        Map<String, Object> variables = new HashMap<>();
        variables.put("amount", loan.getAmount());
        variables.put("employeeLevel", loan.getEmployeeLevel());
        variables.put("approvalLimit", loan.getApprovalLimit());
        variables.put("escalatedApproval", escalated);
        String processInstanceId = processInstanceApi.createProcessInstance(userId,
                new BpmProcessInstanceCreateReqDTO().setProcessDefinitionKey(PROCESS_KEY)
                        .setBusinessKey(String.valueOf(loan.getId())).setVariables(variables)
                        .setStartUserSelectAssignees(request.getStartUserSelectAssignees()));
        loanMapper.updateById(new BpmOALoanDO().setId(loan.getId()).setProcessInstanceId(processInstanceId));
        return loan.getId();
    }

    private EmployeeLimit resolveEmployeeLimit(Long userId) {
        AdminUserRespDTO user = adminUserApi.getUser(userId);
        if (user == null) {
            throw exception(OA_LOAN_USER_NOT_EXISTS);
        }
        Set<String> codes = postApi.getPostList(user.getPostIds()).stream().map(PostRespDTO::getCode)
                .filter(Objects::nonNull).map(code -> code.toLowerCase(Locale.ROOT)).collect(Collectors.toSet());
        if (matches(codes, properties.getDirectorPostCodes())) {
            return new EmployeeLimit("director", properties.getDirectorLimit());
        }
        if (matches(codes, properties.getManagerPostCodes())) {
            return new EmployeeLimit("manager", properties.getManagerLimit());
        }
        return new EmployeeLimit("employee", properties.getEmployeeLimit());
    }

    private static boolean matches(Set<String> actual, Set<String> configured) {
        return configured.stream().filter(Objects::nonNull).map(code -> code.toLowerCase(Locale.ROOT)).anyMatch(actual::contains);
    }

    @Override
    public BpmOALoanDO getLoan(Long userId, Long id) {
        BpmOALoanDO loan = loanMapper.selectById(id);
        if (loan == null || !Objects.equals(loan.getUserId(), userId)) throw exception(OA_LOAN_NOT_EXISTS);
        return loan;
    }

    @Override
    public PageResult<BpmOALoanDO> getLoanPage(Long userId, BpmOALoanPageReqVO request) {
        return loanMapper.selectPage(userId, request);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateLoanStatus(Long id, Integer status) {
        BpmOALoanDO loan = loanMapper.selectByIdForUpdate(id);
        if (loan == null) throw exception(OA_LOAN_NOT_EXISTS);
        if (Objects.equals(loan.getStatus(), status)) return;
        BpmOALoanDO update = new BpmOALoanDO().setId(id).setStatus(status);
        if (BpmTaskStatusEnum.APPROVE.getStatus().equals(status)) {
            update.setOutstandingAmount(loan.getAmount()).setRepaymentStatus(REPAYMENT_OUTSTANDING)
                    .setApprovalTime(LocalDateTime.now());
        } else if (BpmTaskStatusEnum.REJECT.getStatus().equals(status)
                || BpmTaskStatusEnum.CANCEL.getStatus().equals(status)) {
            update.setOutstandingAmount(BigDecimal.ZERO).setRepaymentStatus(REPAYMENT_NONE);
        }
        loanMapper.updateById(update);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createRepayment(Long userId, BpmOALoanRepaymentCreateReqVO request) {
        BpmOALoanDO loan = loanMapper.selectByIdForUpdate(request.getLoanId());
        if (loan == null || !Objects.equals(loan.getUserId(), userId)) throw exception(OA_LOAN_NOT_EXISTS);
        if (!BpmTaskStatusEnum.APPROVE.getStatus().equals(loan.getStatus())
                || !Integer.valueOf(REPAYMENT_OUTSTANDING).equals(loan.getRepaymentStatus())) {
            throw exception(OA_LOAN_REPAYMENT_STATUS_INVALID);
        }
        if (request.getAmount().compareTo(loan.getOutstandingAmount()) > 0) {
            throw exception(OA_LOAN_REPAYMENT_EXCEEDS_OUTSTANDING, loan.getOutstandingAmount());
        }
        LocalDateTime repaidAt = request.getRepaidAt() == null ? LocalDateTime.now() : request.getRepaidAt();
        if (repaidAt.isAfter(LocalDateTime.now())) throw exception(OA_LOAN_REPAYMENT_TIME_INVALID);
        BpmOALoanRepaymentDO repayment = BeanUtils.toBean(request, BpmOALoanRepaymentDO.class)
                .setUserId(userId).setRepaidAt(repaidAt);
        repaymentMapper.insert(repayment);
        BigDecimal remaining = loan.getOutstandingAmount().subtract(request.getAmount());
        BpmOALoanDO update = new BpmOALoanDO().setId(loan.getId()).setOutstandingAmount(remaining);
        if (remaining.signum() == 0) update.setRepaymentStatus(REPAYMENT_PAID).setRepaidTime(repaidAt);
        loanMapper.updateById(update);
        return repayment.getId();
    }

    @Override
    public List<BpmOALoanRepaymentDO> getRepayments(Long userId, Long loanId) {
        getLoan(userId, loanId);
        return repaymentMapper.selectByLoanId(loanId);
    }

    @Override
    public BpmOALoanLimitRespVO getMyLimit(Long userId) {
        EmployeeLimit limit = resolveEmployeeLimit(userId);
        return new BpmOALoanLimitRespVO(limit.level(), limit.limit());
    }

    private record EmployeeLimit(String level, BigDecimal limit) {}
}
