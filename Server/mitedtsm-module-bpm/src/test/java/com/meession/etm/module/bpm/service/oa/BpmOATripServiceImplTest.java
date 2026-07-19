package com.meession.etm.module.bpm.service.oa;

import com.meession.etm.framework.common.exception.ServiceException;
import com.meession.etm.module.bpm.api.task.BpmProcessInstanceApi;
import com.meession.etm.module.bpm.api.task.dto.BpmProcessInstanceCreateReqDTO;
import com.meession.etm.module.bpm.controller.admin.oa.vo.BpmOATripCreateReqVO;
import com.meession.etm.module.bpm.dal.dataobject.oa.BpmOATripDO;
import com.meession.etm.module.bpm.dal.mysql.oa.BpmOATripMapper;
import com.meession.etm.module.bpm.enums.task.BpmTaskStatusEnum;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class BpmOATripServiceImplTest {

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    void calculatesDurationAtHourPrecision() {
        LocalDateTime start = LocalDateTime.of(2026, 8, 1, 8, 0);
        assertEquals(new BigDecimal("1.50"), BpmOATripServiceImpl.calculateDays(start, start.plusHours(36)));
        assertEquals(new BigDecimal("0.05"), BpmOATripServiceImpl.calculateDays(start, start.plusHours(1)));
    }

    @Test
    void requestRejectsPastOrReversedTimeAndShortReason() {
        BpmOATripCreateReqVO request = new BpmOATripCreateReqVO()
                .setStartTime(LocalDateTime.of(2020, 1, 1, 10, 0))
                .setEndTime(LocalDateTime.of(2020, 1, 1, 9, 0))
                .setDestination("上海").setReason("短");
        assertFalse(validator.validate(request).isEmpty());
    }

    @Test
    void creationNormalizesCompanionsAndStartsGovernedProcess() {
        BpmOATripMapper mapper = mock(BpmOATripMapper.class);
        BpmProcessInstanceApi processApi = mock(BpmProcessInstanceApi.class);
        BpmOATripServiceImpl service = service(mapper, processApi);
        doAnswer(invocation -> {
            invocation.<BpmOATripDO>getArgument(0).setId(88L);
            return 1;
        }).when(mapper).insert(any(BpmOATripDO.class));
        when(processApi.createProcessInstance(any(), any())).thenReturn("trip-process-88");
        LocalDateTime start = LocalDateTime.of(2099, 8, 1, 8, 0);
        BpmOATripCreateReqVO request = new BpmOATripCreateReqVO().setStartTime(start)
                .setEndTime(start.plusHours(36)).setDestination("上海").setReason("参加客户项目现场评审")
                .setEstimatedExpense(new BigDecimal("1200.50")).setCompanionUserIds(List.of(7L, 8L, 8L, 9L));

        assertEquals(88L, service.createTrip(7L, request));
        ArgumentCaptor<BpmOATripDO> tripCaptor = ArgumentCaptor.forClass(BpmOATripDO.class);
        verify(mapper).insert(tripCaptor.capture());
        assertEquals(List.of(8L, 9L), tripCaptor.getValue().getCompanionUserIds());
        assertEquals(new BigDecimal("1.50"), tripCaptor.getValue().getDays());
        ArgumentCaptor<BpmProcessInstanceCreateReqDTO> processCaptor = ArgumentCaptor.forClass(BpmProcessInstanceCreateReqDTO.class);
        verify(processApi).createProcessInstance(org.mockito.ArgumentMatchers.eq(7L), processCaptor.capture());
        assertEquals(BpmOATripServiceImpl.PROCESS_KEY, processCaptor.getValue().getProcessDefinitionKey());
        assertEquals("88", processCaptor.getValue().getBusinessKey());
    }

    @Test
    void detailIsRestrictedToApplicantAndApprovalTimeIsPersisted() {
        BpmOATripMapper mapper = mock(BpmOATripMapper.class);
        BpmOATripServiceImpl service = service(mapper, mock(BpmProcessInstanceApi.class));
        BpmOATripDO trip = new BpmOATripDO().setId(3L).setUserId(9L);
        when(mapper.selectById(3L)).thenReturn(trip);
        assertThrows(ServiceException.class, () -> service.getTrip(8L, 3L));

        service.updateTripStatus(3L, BpmTaskStatusEnum.APPROVE.getStatus());
        ArgumentCaptor<BpmOATripDO> update = ArgumentCaptor.forClass(BpmOATripDO.class);
        verify(mapper).updateById(update.capture());
        assertEquals(BpmTaskStatusEnum.APPROVE.getStatus(), update.getValue().getStatus());
        org.junit.jupiter.api.Assertions.assertNotNull(update.getValue().getApprovalTime());
    }

    @Test
    void reimbursementRequiresApprovedEndedTripAndCoveringExpenseRange() {
        BpmOATripMapper mapper = mock(BpmOATripMapper.class);
        BpmOATripServiceImpl service = service(mapper, mock(BpmProcessInstanceApi.class));
        BpmOATripDO trip = new BpmOATripDO().setId(6L).setUserId(7L)
                .setStatus(BpmTaskStatusEnum.APPROVE.getStatus())
                .setStartTime(LocalDateTime.now().minusDays(3))
                .setEndTime(LocalDateTime.now().minusDays(1));
        when(mapper.selectById(6L)).thenReturn(trip);

        assertEquals(trip, service.validateReimbursableTrip(7L, 6L,
                LocalDate.now().minusDays(3), LocalDate.now()));
        assertThrows(ServiceException.class, () -> service.validateReimbursableTrip(7L, 6L,
                LocalDate.now().minusDays(2), LocalDate.now()));
        trip.setStatus(BpmTaskStatusEnum.RUNNING.getStatus());
        assertThrows(ServiceException.class, () -> service.validateReimbursableTrip(7L, 6L,
                LocalDate.now().minusDays(3), LocalDate.now()));
    }

    private static BpmOATripServiceImpl service(BpmOATripMapper mapper, BpmProcessInstanceApi processApi) {
        BpmOATripServiceImpl service = new BpmOATripServiceImpl();
        ReflectionTestUtils.setField(service, "tripMapper", mapper);
        ReflectionTestUtils.setField(service, "processInstanceApi", processApi);
        return service;
    }
}
