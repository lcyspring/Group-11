package com.meession.etm.module.bpm.service.oa;

import com.meession.etm.module.bpm.controller.admin.oa.vo.BpmOALeaveCreateReqVO;
import com.meession.etm.module.bpm.api.task.BpmProcessInstanceApi;
import com.meession.etm.module.bpm.dal.dataobject.oa.BpmOALeaveBalanceDO;
import com.meession.etm.module.bpm.dal.dataobject.oa.BpmOALeaveDO;
import com.meession.etm.module.bpm.dal.mysql.oa.BpmOALeaveBalanceMapper;
import com.meession.etm.module.bpm.dal.mysql.oa.BpmOALeaveCalendarMapper;
import com.meession.etm.module.bpm.dal.mysql.oa.BpmOALeaveMapper;
import com.meession.etm.module.bpm.enums.task.BpmTaskStatusEnum;
import com.meession.etm.module.bpm.framework.oa.BpmOALeaveProperties;
import com.meession.etm.framework.tenant.core.context.TenantContextHolder;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import org.mockito.ArgumentCaptor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class BpmOALeaveServiceImplTest {

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    @AfterEach
    void clearTenant() {
        TenantContextHolder.clear();
    }

    @Test
    void calculateWorkingDaysIncludesWeekdaysAndExcludesWeekend() {
        assertEquals(1, BpmOALeaveServiceImpl.calculateWorkingDays(
                LocalDate.of(2026, 7, 17), LocalDate.of(2026, 7, 17)));
        assertEquals(2, BpmOALeaveServiceImpl.calculateWorkingDays(
                LocalDate.of(2026, 7, 17), LocalDate.of(2026, 7, 20)));
        assertEquals(2, BpmOALeaveServiceImpl.calculateWorkingDays(
                LocalDate.of(2026, 7, 18), LocalDate.of(2026, 7, 19),
                Map.of(LocalDate.of(2026, 7, 18), true, LocalDate.of(2026, 7, 19), true)));
        assertEquals(0, BpmOALeaveServiceImpl.calculateWorkingDays(
                LocalDate.of(2026, 7, 20), LocalDate.of(2026, 7, 20),
                Map.of(LocalDate.of(2026, 7, 20), false)));
    }

    @Test
    void requestRequiresStrictTimeOrderAndMeaningfulReason() {
        LocalDateTime time = LocalDateTime.of(2026, 7, 17, 9, 0);
        BpmOALeaveCreateReqVO request = new BpmOALeaveCreateReqVO()
                .setType(1).setReason("太短").setStartTime(time).setEndTime(time);

        assertFalse(validator.validate(request).isEmpty());
    }

    @Test
    void approvalConvertsReservationExactlyOnce() {
        BpmOALeaveMapper leaveMapper = mock(BpmOALeaveMapper.class);
        BpmOALeaveBalanceMapper balanceMapper = mock(BpmOALeaveBalanceMapper.class);
        BpmOALeaveServiceImpl service = service(leaveMapper, balanceMapper, mock(BpmOALeaveCalendarMapper.class));
        TenantContextHolder.setTenantId(1L);

        BpmOALeaveDO reserved = new BpmOALeaveDO().setId(10L).setUserId(20L).setType("4")
                .setStartTime(LocalDateTime.of(2026, 7, 20, 9, 0)).setDay(2L)
                .setBalanceReserved(true).setBalanceDeducted(false);
        BpmOALeaveDO settled = new BpmOALeaveDO().setId(10L).setUserId(20L).setType("4")
                .setStartTime(reserved.getStartTime()).setDay(2L)
                .setBalanceReserved(false).setBalanceDeducted(true);
        when(leaveMapper.selectByIdForUpdate(10L)).thenReturn(reserved, settled);
        when(balanceMapper.selectForUpdate(20L, 4, 2026, 1L)).thenReturn(new BpmOALeaveBalanceDO()
                .setId(30L).setTotalDays(5L).setReservedDays(2L).setUsedDays(0L));

        service.updateLeaveStatus(10L, BpmTaskStatusEnum.APPROVE.getStatus());
        service.updateLeaveStatus(10L, BpmTaskStatusEnum.APPROVE.getStatus());

        ArgumentCaptor<BpmOALeaveBalanceDO> captor = ArgumentCaptor.forClass(BpmOALeaveBalanceDO.class);
        verify(balanceMapper, times(1)).updateById((BpmOALeaveBalanceDO) captor.capture());
        assertEquals(0L, captor.getValue().getReservedDays());
        assertEquals(2L, captor.getValue().getUsedDays());
    }

    @Test
    void submissionRejectsInsufficientAnnualBalanceBeforeCreatingBusinessRow() {
        BpmOALeaveMapper leaveMapper = mock(BpmOALeaveMapper.class);
        BpmOALeaveBalanceMapper balanceMapper = mock(BpmOALeaveBalanceMapper.class);
        BpmOALeaveCalendarMapper calendarMapper = mock(BpmOALeaveCalendarMapper.class);
        BpmOALeaveServiceImpl service = service(leaveMapper, balanceMapper, calendarMapper);
        TenantContextHolder.setTenantId(1L);
        when(calendarMapper.selectRange(any(), any())).thenReturn(java.util.List.of());
        when(balanceMapper.selectForUpdate(20L, 4, 2026, 1L)).thenReturn(new BpmOALeaveBalanceDO()
                .setId(30L).setTotalDays(1L).setReservedDays(0L).setUsedDays(0L));
        BpmOALeaveCreateReqVO request = new BpmOALeaveCreateReqVO().setType(4)
                .setReason("年度休假余额不足验证申请")
                .setStartTime(LocalDateTime.of(2026, 7, 20, 9, 0))
                .setEndTime(LocalDateTime.of(2026, 7, 21, 18, 0));

        assertThrows(RuntimeException.class, () -> service.createLeave(20L, request));
        verify(leaveMapper, never()).insert(any(BpmOALeaveDO.class));
    }

    private static BpmOALeaveServiceImpl service(BpmOALeaveMapper leaveMapper,
                                                   BpmOALeaveBalanceMapper balanceMapper,
                                                   BpmOALeaveCalendarMapper calendarMapper) {
        BpmOALeaveServiceImpl service = new BpmOALeaveServiceImpl();
        BpmOALeaveProperties properties = new BpmOALeaveProperties();
        properties.setBalanceRequiredTypes(Set.of(4, 5));
        properties.setDefaultAnnualDays(Map.of(4, 5L, 5, 0L));
        ReflectionTestUtils.setField(service, "leaveMapper", leaveMapper);
        ReflectionTestUtils.setField(service, "balanceMapper", balanceMapper);
        ReflectionTestUtils.setField(service, "calendarMapper", calendarMapper);
        ReflectionTestUtils.setField(service, "leaveProperties", properties);
        ReflectionTestUtils.setField(service, "processInstanceApi", mock(BpmProcessInstanceApi.class));
        return service;
    }
}
