package com.meession.etm.module.bpm.service.oa;

import com.meession.etm.framework.common.pojo.PageResult;
import com.meession.etm.framework.common.util.object.BeanUtils;
import com.meession.etm.module.bpm.api.task.BpmProcessInstanceApi;
import com.meession.etm.module.bpm.api.task.dto.BpmProcessInstanceCreateReqDTO;
import com.meession.etm.module.bpm.controller.admin.oa.vo.BpmOALeaveCreateReqVO;
import com.meession.etm.module.bpm.controller.admin.oa.vo.BpmOALeavePageReqVO;
import com.meession.etm.module.bpm.dal.dataobject.oa.BpmOALeaveDO;
import com.meession.etm.module.bpm.dal.dataobject.oa.BpmOALeaveBalanceDO;
import com.meession.etm.module.bpm.dal.dataobject.oa.BpmOALeaveCalendarDO;
import com.meession.etm.module.bpm.dal.mysql.oa.BpmOALeaveBalanceMapper;
import com.meession.etm.module.bpm.dal.mysql.oa.BpmOALeaveCalendarMapper;
import com.meession.etm.module.bpm.dal.mysql.oa.BpmOALeaveMapper;
import com.meession.etm.module.bpm.enums.task.BpmTaskStatusEnum;
import com.meession.etm.module.bpm.framework.oa.BpmOALeaveProperties;
import com.meession.etm.framework.tenant.core.context.TenantContextHolder;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

import static com.meession.etm.framework.common.exception.util.ServiceExceptionUtil.exception;
import static com.meession.etm.module.bpm.enums.ErrorCodeConstants.OA_LEAVE_NOT_EXISTS;
import static com.meession.etm.module.bpm.enums.ErrorCodeConstants.OA_LEAVE_BALANCE_CONFIG_MISSING;
import static com.meession.etm.module.bpm.enums.ErrorCodeConstants.OA_LEAVE_BALANCE_INSUFFICIENT;
import static com.meession.etm.module.bpm.enums.ErrorCodeConstants.OA_LEAVE_NO_WORKDAY;

/**
 * OA 请假申请 Service 实现类
 *
 * @author jason
 * @author 密讯
 */
@Service
@Validated
public class BpmOALeaveServiceImpl implements BpmOALeaveService {

    /**
     * OA 请假对应的流程定义 KEY
     */
    public static final String PROCESS_KEY = "oa_leave";

    @Resource
    private BpmOALeaveMapper leaveMapper;

    @Resource
    private BpmProcessInstanceApi processInstanceApi;

    @Resource
    private BpmOALeaveCalendarMapper calendarMapper;

    @Resource
    private BpmOALeaveBalanceMapper balanceMapper;

    @Resource
    private BpmOALeaveProperties leaveProperties;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createLeave(Long userId, BpmOALeaveCreateReqVO createReqVO) {
        // 插入 OA 请假单
        LocalDate startDate = createReqVO.getStartTime().toLocalDate();
        LocalDate endDate = createReqVO.getEndTime().toLocalDate();
        Map<LocalDate, Boolean> calendarOverrides = calendarMapper.selectRange(startDate, endDate).stream()
                .collect(Collectors.toMap(BpmOALeaveCalendarDO::getCalendarDate,
                        BpmOALeaveCalendarDO::getWorkday, (left, right) -> right));
        long day = calculateWorkingDays(startDate, endDate, calendarOverrides);
        if (day <= 0) {
            throw exception(OA_LEAVE_NO_WORKDAY);
        }
        boolean balanceReserved = reserveBalanceIfRequired(userId, createReqVO.getType(), startDate.getYear(), day);
        BpmOALeaveDO leave = BeanUtils.toBean(createReqVO, BpmOALeaveDO.class)
                .setUserId(userId).setDay(day).setStatus(BpmTaskStatusEnum.RUNNING.getStatus())
                .setBalanceReserved(balanceReserved).setBalanceDeducted(false);
        leaveMapper.insert(leave);

        // 发起 BPM 流程
        Map<String, Object> processInstanceVariables = new HashMap<>();
        processInstanceVariables.put("day", day);
        String processInstanceId = processInstanceApi.createProcessInstance(userId,
                new BpmProcessInstanceCreateReqDTO().setProcessDefinitionKey(PROCESS_KEY)
                        .setVariables(processInstanceVariables).setBusinessKey(String.valueOf(leave.getId()))
                        .setStartUserSelectAssignees(createReqVO.getStartUserSelectAssignees()));

        // 将工作流的编号，更新到 OA 请假单中
        leaveMapper.updateById(new BpmOALeaveDO().setId(leave.getId()).setProcessInstanceId(processInstanceId));
        return leave.getId();
    }

    static long calculateWorkingDays(LocalDate startDate, LocalDate endDate) {
        return calculateWorkingDays(startDate, endDate, Map.of());
    }

    static long calculateWorkingDays(LocalDate startDate, LocalDate endDate,
                                     Map<LocalDate, Boolean> calendarOverrides) {
        long days = 0;
        for (LocalDate date = startDate; !date.isAfter(endDate); date = date.plusDays(1)) {
            boolean defaultWorkday = date.getDayOfWeek() != DayOfWeek.SATURDAY
                    && date.getDayOfWeek() != DayOfWeek.SUNDAY;
            if (calendarOverrides.getOrDefault(date, defaultWorkday)) {
                days++;
            }
        }
        return days;
    }

    private boolean reserveBalanceIfRequired(Long userId, Integer leaveType, Integer year, long days) {
        if (!isBalanceRequired(leaveType)) {
            return false;
        }
        BpmOALeaveBalanceDO balance = getOrCreateBalanceForUpdate(userId, leaveType, year);
        if (balance.availableDays() < days) {
            throw exception(OA_LEAVE_BALANCE_INSUFFICIENT, balance.availableDays(), days);
        }
        balanceMapper.updateById(new BpmOALeaveBalanceDO().setId(balance.getId())
                .setReservedDays(balance.getReservedDays() + days));
        return true;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateLeaveStatus(Long id, Integer status) {
        BpmOALeaveDO leave = leaveMapper.selectByIdForUpdate(id);
        if (leave == null) {
            throw exception(OA_LEAVE_NOT_EXISTS);
        }
        BpmOALeaveDO update = new BpmOALeaveDO().setId(id).setStatus(status);
        if (Boolean.TRUE.equals(leave.getBalanceReserved()) && !Boolean.TRUE.equals(leave.getBalanceDeducted())) {
            BpmOALeaveBalanceDO balance = getOrCreateBalanceForUpdate(leave.getUserId(),
                    Integer.valueOf(leave.getType()), leave.getStartTime().getYear());
            if (BpmTaskStatusEnum.APPROVE.getStatus().equals(status)) {
                balanceMapper.updateById(new BpmOALeaveBalanceDO().setId(balance.getId())
                        .setReservedDays(Math.max(0, balance.getReservedDays() - leave.getDay()))
                        .setUsedDays(balance.getUsedDays() + leave.getDay()));
                update.setBalanceReserved(false).setBalanceDeducted(true);
            } else if (BpmTaskStatusEnum.REJECT.getStatus().equals(status)
                    || BpmTaskStatusEnum.CANCEL.getStatus().equals(status)) {
                balanceMapper.updateById(new BpmOALeaveBalanceDO().setId(balance.getId())
                        .setReservedDays(Math.max(0, balance.getReservedDays() - leave.getDay())));
                update.setBalanceReserved(false);
            }
        }
        leaveMapper.updateById(update);
    }

    private void validateLeaveExists(Long id) {
        if (leaveMapper.selectById(id) == null) {
            throw exception(OA_LEAVE_NOT_EXISTS);
        }
    }

    @Override
    public BpmOALeaveDO getLeave(Long id) {
        return leaveMapper.selectById(id);
    }

    @Override
    public PageResult<BpmOALeaveDO> getLeavePage(Long userId, BpmOALeavePageReqVO pageReqVO) {
        return leaveMapper.selectPage(userId, pageReqVO);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public BpmOALeaveBalanceDO getOrCreateBalance(Long userId, Integer leaveType, Integer year) {
        return getOrCreateBalanceForUpdate(userId, leaveType, year);
    }

    private BpmOALeaveBalanceDO getOrCreateBalanceForUpdate(Long userId, Integer leaveType, Integer year) {
        Long defaultDays = leaveProperties.getDefaultAnnualDays().get(leaveType);
        if (defaultDays == null) {
            throw exception(OA_LEAVE_BALANCE_CONFIG_MISSING, leaveType);
        }
        Long tenantId = TenantContextHolder.getRequiredTenantId();
        balanceMapper.insertDefault(userId, leaveType, year, defaultDays, tenantId);
        return balanceMapper.selectForUpdate(userId, leaveType, year, tenantId);
    }

    @Override
    public boolean isBalanceRequired(Integer leaveType) {
        return leaveProperties.getBalanceRequiredTypes().contains(leaveType);
    }

}
