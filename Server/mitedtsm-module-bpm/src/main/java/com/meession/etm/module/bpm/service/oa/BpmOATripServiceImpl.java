package com.meession.etm.module.bpm.service.oa;

import com.meession.etm.framework.common.pojo.PageResult;
import com.meession.etm.framework.common.util.object.BeanUtils;
import com.meession.etm.module.bpm.api.task.BpmProcessInstanceApi;
import com.meession.etm.module.bpm.api.task.dto.BpmProcessInstanceCreateReqDTO;
import com.meession.etm.module.bpm.controller.admin.oa.vo.BpmOATripCreateReqVO;
import com.meession.etm.module.bpm.controller.admin.oa.vo.BpmOATripPageReqVO;
import com.meession.etm.module.bpm.dal.dataobject.oa.BpmOATripDO;
import com.meession.etm.module.bpm.dal.mysql.oa.BpmOATripMapper;
import com.meession.etm.module.bpm.enums.task.BpmTaskStatusEnum;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static com.meession.etm.framework.common.exception.util.ServiceExceptionUtil.exception;
import static com.meession.etm.module.bpm.enums.ErrorCodeConstants.OA_TRIP_NOT_EXISTS;

@Service
@Validated
public class BpmOATripServiceImpl implements BpmOATripService {

    public static final String PROCESS_KEY = "oa_trip";

    @Resource
    private BpmOATripMapper tripMapper;
    @Resource
    private BpmProcessInstanceApi processInstanceApi;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createTrip(Long userId, BpmOATripCreateReqVO request) {
        List<Long> companionUserIds = request.getCompanionUserIds() == null ? List.of()
                : request.getCompanionUserIds().stream().filter(Objects::nonNull)
                .filter(id -> !Objects.equals(id, userId)).distinct().toList();
        BigDecimal days = calculateDays(request.getStartTime(), request.getEndTime());
        BpmOATripDO trip = BeanUtils.toBean(request, BpmOATripDO.class)
                .setUserId(userId).setDays(days).setCompanionUserIds(companionUserIds)
                .setEstimatedExpense(request.getEstimatedExpense() == null
                        ? BigDecimal.ZERO : request.getEstimatedExpense())
                .setStatus(BpmTaskStatusEnum.RUNNING.getStatus());
        tripMapper.insert(trip);

        Map<String, Object> variables = new HashMap<>();
        variables.put("days", days);
        variables.put("estimatedExpense", trip.getEstimatedExpense());
        variables.put("destination", trip.getDestination());
        String processInstanceId = processInstanceApi.createProcessInstance(userId,
                new BpmProcessInstanceCreateReqDTO().setProcessDefinitionKey(PROCESS_KEY)
                        .setBusinessKey(String.valueOf(trip.getId())).setVariables(variables)
                        .setStartUserSelectAssignees(request.getStartUserSelectAssignees()));
        tripMapper.updateById(new BpmOATripDO().setId(trip.getId()).setProcessInstanceId(processInstanceId));
        return trip.getId();
    }

    static BigDecimal calculateDays(LocalDateTime startTime, LocalDateTime endTime) {
        long minutes = Duration.between(startTime, endTime).toMinutes();
        return BigDecimal.valueOf(minutes).divide(BigDecimal.valueOf(24L * 60L), 2, RoundingMode.UP);
    }

    @Override
    public BpmOATripDO getTrip(Long userId, Long id) {
        BpmOATripDO trip = tripMapper.selectById(id);
        if (trip == null || !Objects.equals(trip.getUserId(), userId)) {
            throw exception(OA_TRIP_NOT_EXISTS);
        }
        return trip;
    }

    @Override
    public PageResult<BpmOATripDO> getTripPage(Long userId, BpmOATripPageReqVO request) {
        return tripMapper.selectPage(userId, request);
    }

    @Override
    public void updateTripStatus(Long id, Integer status) {
        BpmOATripDO trip = tripMapper.selectById(id);
        if (trip == null) {
            throw exception(OA_TRIP_NOT_EXISTS);
        }
        BpmOATripDO update = new BpmOATripDO().setId(id).setStatus(status);
        if (BpmTaskStatusEnum.APPROVE.getStatus().equals(status)
                || BpmTaskStatusEnum.REJECT.getStatus().equals(status)) {
            update.setApprovalTime(LocalDateTime.now());
        }
        tripMapper.updateById(update);
    }

    @Override
    public List<BpmOATripDO> getReimbursableTrips(Long userId) {
        return tripMapper.selectReimbursable(userId, BpmTaskStatusEnum.APPROVE.getStatus(), LocalDateTime.now());
    }

    @Override
    public BpmOATripDO validateReimbursableTrip(Long userId, Long id,
                                                LocalDate expenseStartDate, LocalDate expenseEndDate) {
        BpmOATripDO trip = getTrip(userId, id);
        if (!BpmTaskStatusEnum.APPROVE.getStatus().equals(trip.getStatus())) {
            throw exception(com.meession.etm.module.bpm.enums.ErrorCodeConstants.OA_TRIP_NOT_APPROVED);
        }
        if (trip.getEndTime().isAfter(LocalDateTime.now())) {
            throw exception(com.meession.etm.module.bpm.enums.ErrorCodeConstants.OA_TRIP_NOT_ENDED);
        }
        if (expenseStartDate.isAfter(trip.getStartTime().toLocalDate())
                || expenseEndDate.isBefore(trip.getEndTime().toLocalDate())) {
            throw exception(com.meession.etm.module.bpm.enums.ErrorCodeConstants.OA_TRIP_EXPENSE_RANGE_INVALID);
        }
        return trip;
    }
}
