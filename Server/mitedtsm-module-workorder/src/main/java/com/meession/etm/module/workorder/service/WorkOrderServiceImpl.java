package com.meession.etm.module.workorder.service;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.collection.ListUtil;
import com.meession.etm.framework.common.pojo.PageResult;
import com.meession.etm.framework.common.util.object.BeanUtils;
import com.meession.etm.module.workorder.controller.admin.workorder.vo.workorder.WorkOrderAssignReqVO;
import com.meession.etm.module.workorder.controller.admin.workorder.vo.workorder.WorkOrderCompleteReqVO;
import com.meession.etm.module.workorder.controller.admin.workorder.vo.workorder.WorkOrderEfficiencyRespVO;
import com.meession.etm.module.workorder.controller.admin.workorder.vo.workorder.WorkOrderPageReqVO;
import com.meession.etm.module.workorder.controller.admin.workorder.vo.workorder.WorkOrderProcessReqVO;
import com.meession.etm.module.workorder.controller.admin.workorder.vo.workorder.WorkOrderReturnReqVO;
import com.meession.etm.module.workorder.controller.admin.workorder.vo.workorder.WorkOrderSaveReqVO;
import com.meession.etm.module.workorder.controller.admin.workorder.vo.workorder.WorkOrderStatisticsRespVO;
import com.meession.etm.module.workorder.controller.admin.workorder.vo.workorder.WorkOrderTrendReqVO;
import com.meession.etm.module.workorder.controller.admin.workorder.vo.workorder.WorkOrderTrendRespVO;
import com.meession.etm.module.workorder.controller.admin.workorder.vo.workorder.WorkOrderUpdatePriorityReqVO;
import com.meession.etm.module.workorder.controller.admin.workorder.vo.workorder.WorkOrderUpdateStatusReqVO;
import com.meession.etm.module.workorder.dal.dataobject.WorkOrderDO;
import com.meession.etm.module.workorder.dal.mysql.WorkOrderMapper;
import com.meession.etm.module.workorder.enums.WorkOrderPriorityEnum;
import com.meession.etm.module.workorder.enums.WorkOrderStatusEnum;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

import static com.meession.etm.framework.common.exception.util.ServiceExceptionUtil.exception;
import static com.meession.etm.module.workorder.enums.ErrorCodeConstants.*;

/**
 * 工单 Service 实现类
 *
 * @author fwx
 */
@Service
@Validated
public class WorkOrderServiceImpl implements WorkOrderService {

    @Resource
    private WorkOrderMapper workOrderMapper;

    @Resource
    private WorkOrderTypeService workOrderTypeService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createWorkOrder(WorkOrderSaveReqVO createReqVO, Long userId) {
        // 1. 校验工单类型
        if (createReqVO.getTypeId() != null) {
            workOrderTypeService.validateWorkOrderTypeExists(createReqVO.getTypeId());
        }

        // 2. 插入工单
        WorkOrderDO workOrder = BeanUtils.toBean(createReqVO, WorkOrderDO.class);
        workOrder.setSubmitterUserId(userId);
        workOrder.setStatus(WorkOrderStatusEnum.PENDING.getStatus()); // 默认状态：待处理
        if (workOrder.getPriority() == null) {
            workOrder.setPriority(0); // 默认低优先级
        }
        workOrderMapper.insert(workOrder);

        return workOrder.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateWorkOrder(WorkOrderSaveReqVO updateReqVO) {
        // 1. 校验存在
        WorkOrderDO oldWorkOrder = validateWorkOrderExists(updateReqVO.getId());

        // 2. 校验工单类型
        if (updateReqVO.getTypeId() != null) {
            workOrderTypeService.validateWorkOrderTypeExists(updateReqVO.getTypeId());
        }

        // 3. 更新工单（不允许更新状态字段，状态变更使用专用接口）
        WorkOrderDO updateObj = BeanUtils.toBean(updateReqVO, WorkOrderDO.class);
        updateObj.setStatus(null); // 不允许通过此接口更新状态
        workOrderMapper.updateById(updateObj);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateWorkOrderStatus(WorkOrderUpdateStatusReqVO reqVO) {
        // 1.1 校验存在
        WorkOrderDO workOrder = validateWorkOrderExists(reqVO.getId());
        // 1.2 校验状态是否允许流转
        if (!WorkOrderStatusEnum.canTransition(workOrder.getStatus(), reqVO.getStatus())) {
            if (workOrder.getStatus().equals(reqVO.getStatus())) {
                throw exception(WORK_ORDER_UPDATE_STATUS_FAIL_STATUS_EQUALS);
            }
            throw exception(WORK_ORDER_UPDATE_STATUS_FAIL_END_STATUS);
        }

        // 2. 更新状态
        WorkOrderDO updateObj = new WorkOrderDO()
                .setId(reqVO.getId())
                .setStatus(reqVO.getStatus());
        // 如果流转到处理中，设置处理时间和处理人
        if (WorkOrderStatusEnum.PROCESSING.getStatus().equals(reqVO.getStatus())) {
            updateObj.setHandleTime(LocalDateTime.now());
        }
        // 如果流转到已完成或已关闭，设置完成时间
        if (WorkOrderStatusEnum.COMPLETED.getStatus().equals(reqVO.getStatus())
                || WorkOrderStatusEnum.CLOSED.getStatus().equals(reqVO.getStatus())) {
            updateObj.setFinishTime(LocalDateTime.now());
        }
        // 处理结果
        if (reqVO.getResult() != null) {
            updateObj.setResult(reqVO.getResult());
        }
        workOrderMapper.updateById(updateObj);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateWorkOrderPriority(WorkOrderUpdatePriorityReqVO reqVO) {
        // 1. 校验存在
        validateWorkOrderExists(reqVO.getId());
        // 2. 更新优先级
        WorkOrderDO updateObj = new WorkOrderDO()
                .setId(reqVO.getId())
                .setPriority(reqVO.getPriority());
        workOrderMapper.updateById(updateObj);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void assignWorkOrder(WorkOrderAssignReqVO reqVO) {
        // 1. 校验存在
        WorkOrderDO workOrder = validateWorkOrderExists(reqVO.getId());
        // 2. 校验状态（只有待处理或已退回状态才能分配）
        if (!WorkOrderStatusEnum.PENDING.getStatus().equals(workOrder.getStatus())
                && !WorkOrderStatusEnum.RETURNED.getStatus().equals(workOrder.getStatus())) {
            throw exception(WORK_ORDER_ASSIGN_FAIL_STATUS);
        }
        // 3. 更新处理人
        WorkOrderDO updateObj = new WorkOrderDO()
                .setId(reqVO.getId())
                .setHandlerUserId(reqVO.getHandlerUserId());
        workOrderMapper.updateById(updateObj);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void processWorkOrder(WorkOrderProcessReqVO reqVO, Long userId) {
        // 1. 校验存在
        WorkOrderDO workOrder = validateWorkOrderExists(reqVO.getId());
        // 2. 校验状态（只有待处理或已退回状态才能开始处理）
        if (!WorkOrderStatusEnum.PENDING.getStatus().equals(workOrder.getStatus())
                && !WorkOrderStatusEnum.RETURNED.getStatus().equals(workOrder.getStatus())) {
            throw exception(WORK_ORDER_PROCESS_FAIL_STATUS);
        }
        // 3. 更新为处理中
        WorkOrderDO updateObj = new WorkOrderDO()
                .setId(reqVO.getId())
                .setStatus(WorkOrderStatusEnum.PROCESSING.getStatus())
                .setHandleTime(LocalDateTime.now());
        // 如果之前没有分配处理人，自动将当前用户设为处理人
        if (workOrder.getHandlerUserId() == null) {
            updateObj.setHandlerUserId(userId);
        }
        // 处理备注
        if (reqVO.getResult() != null) {
            updateObj.setResult(reqVO.getResult());
        }
        workOrderMapper.updateById(updateObj);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void completeWorkOrder(WorkOrderCompleteReqVO reqVO) {
        // 1. 校验存在
        WorkOrderDO workOrder = validateWorkOrderExists(reqVO.getId());
        // 2. 校验状态（只有处理中状态才能完结）
        if (!WorkOrderStatusEnum.PROCESSING.getStatus().equals(workOrder.getStatus())) {
            throw exception(WORK_ORDER_COMPLETE_FAIL_STATUS);
        }
        // 3. 更新为已完成
        WorkOrderDO updateObj = new WorkOrderDO()
                .setId(reqVO.getId())
                .setStatus(WorkOrderStatusEnum.COMPLETED.getStatus())
                .setResult(reqVO.getResult())
                .setFinishTime(LocalDateTime.now());
        workOrderMapper.updateById(updateObj);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void returnWorkOrder(WorkOrderReturnReqVO reqVO) {
        // 1. 校验存在
        WorkOrderDO workOrder = validateWorkOrderExists(reqVO.getId());
        // 2. 校验状态（只有处理中状态才能退回）
        if (!WorkOrderStatusEnum.PROCESSING.getStatus().equals(workOrder.getStatus())) {
            throw exception(WORK_ORDER_RETURN_FAIL_STATUS);
        }
        // 3. 更新为已退回，记录退回原因
        WorkOrderDO updateObj = new WorkOrderDO()
                .setId(reqVO.getId())
                .setStatus(WorkOrderStatusEnum.RETURNED.getStatus())
                .setResult(reqVO.getResult());
        workOrderMapper.updateById(updateObj);
    }

    @Override
    public WorkOrderStatisticsRespVO getWorkOrderStatistics() {
        List<WorkOrderDO> allOrders = workOrderMapper.selectAll();
        LocalDate today = LocalDate.now();

        WorkOrderStatisticsRespVO vo = new WorkOrderStatisticsRespVO();
        vo.setTotalCount((long) allOrders.size());

        // 今日新增和今日完结
        long todayNew = allOrders.stream()
                .filter(o -> o.getCreateTime() != null && o.getCreateTime().toLocalDate().equals(today))
                .count();
        long todayCompleted = allOrders.stream()
                .filter(o -> o.getFinishTime() != null && o.getFinishTime().toLocalDate().equals(today))
                .count();
        vo.setTodayNewCount(todayNew);
        vo.setTodayCompletedCount(todayCompleted);

        // 状态分布
        Map<String, Long> statusDist = new LinkedHashMap<>();
        for (WorkOrderStatusEnum statusEnum : WorkOrderStatusEnum.values()) {
            long count = allOrders.stream().filter(o -> statusEnum.getStatus().equals(o.getStatus())).count();
            statusDist.put(statusEnum.getName(), count);
        }
        vo.setStatusDistribution(statusDist);

        // 优先级分布
        Map<String, Long> priorityDist = new LinkedHashMap<>();
        for (WorkOrderPriorityEnum priorityEnum : WorkOrderPriorityEnum.values()) {
            long count = allOrders.stream().filter(o -> priorityEnum.getPriority().equals(o.getPriority())).count();
            priorityDist.put(priorityEnum.getName(), count);
        }
        vo.setPriorityDistribution(priorityDist);

        // 类型分布
        Map<String, Long> typeDist = workOrderTypeService.getWorkOrderTypeList(
                allOrders.stream().map(WorkOrderDO::getTypeId).filter(Objects::nonNull).collect(Collectors.toSet()))
                .stream().collect(Collectors.toMap(
                        t -> t.getName(),
                        t -> allOrders.stream().filter(o -> t.getId().equals(o.getTypeId())).count(),
                        (a, b) -> a, LinkedHashMap::new));
        vo.setTypeDistribution(typeDist);

        return vo;
    }

    @Override
    public WorkOrderEfficiencyRespVO getWorkOrderEfficiencyAnalysis() {
        List<WorkOrderDO> allOrders = workOrderMapper.selectAll();

        // 筛选已完结且有处理时间的工单
        List<WorkOrderDO> completedOrders = allOrders.stream()
                .filter(o -> (WorkOrderStatusEnum.COMPLETED.getStatus().equals(o.getStatus())
                        || WorkOrderStatusEnum.CLOSED.getStatus().equals(o.getStatus()))
                        && o.getHandleTime() != null && o.getFinishTime() != null)
                .collect(Collectors.toList());

        WorkOrderEfficiencyRespVO vo = new WorkOrderEfficiencyRespVO();
        vo.setCompletedCount((long) completedOrders.size());

        if (!completedOrders.isEmpty()) {
            List<Double> durations = completedOrders.stream()
                    .map(o -> (double) ChronoUnit.MINUTES.between(o.getHandleTime(), o.getFinishTime()) / 60.0)
                    .sorted()
                    .collect(Collectors.toList());

            double avg = durations.stream().mapToDouble(Double::doubleValue).average().orElse(0);
            double median = durations.size() % 2 == 0
                    ? (durations.get(durations.size() / 2 - 1) + durations.get(durations.size() / 2)) / 2.0
                    : durations.get(durations.size() / 2);

            vo.setAvgProcessingHours(Math.round(avg * 10.0) / 10.0);
            vo.setMedianProcessingHours(Math.round(median * 10.0) / 10.0);
            vo.setMinProcessingHours(Math.round(durations.get(0) * 10.0) / 10.0);
            vo.setMaxProcessingHours(Math.round(durations.get(durations.size() - 1) * 10.0) / 10.0);

            // 按时完成
            long onTime = completedOrders.stream()
                    .filter(o -> o.getExpectedFinishTime() != null
                            && !o.getExpectedFinishTime().isBefore(o.getFinishTime()))
                    .count();
            long delayed = completedOrders.size() - onTime;
            vo.setOnTimeCount(onTime);
            vo.setDelayedCount(delayed);
            vo.setOnTimeRate(completedOrders.size() > 0
                    ? Math.round((double) onTime / completedOrders.size() * 1000.0) / 10.0 : 0.0);

            // 按处理人
            Map<String, Double> byHandler = completedOrders.stream()
                    .filter(o -> o.getHandlerUserId() != null)
                    .collect(Collectors.groupingBy(
                            o -> "用户" + o.getHandlerUserId(),
                            Collectors.averagingDouble(o ->
                                    (double) ChronoUnit.MINUTES.between(o.getHandleTime(), o.getFinishTime()) / 60.0)));
            vo.setAvgProcessingHoursByHandler(byHandler.entrySet().stream()
                    .collect(Collectors.toMap(Map.Entry::getKey,
                            e -> Math.round(e.getValue() * 10.0) / 10.0,
                            (a, b) -> a, LinkedHashMap::new)));

            // 按优先级
            Map<String, Double> byPriority = new LinkedHashMap<>();
            for (WorkOrderPriorityEnum p : WorkOrderPriorityEnum.values()) {
                List<WorkOrderDO> byP = completedOrders.stream()
                        .filter(o -> p.getPriority().equals(o.getPriority()))
                        .collect(Collectors.toList());
                if (!byP.isEmpty()) {
                    double avgP = byP.stream()
                            .mapToDouble(o -> (double) ChronoUnit.MINUTES.between(o.getHandleTime(), o.getFinishTime()) / 60.0)
                            .average().orElse(0);
                    byPriority.put(p.getName(), Math.round(avgP * 10.0) / 10.0);
                }
            }
            vo.setAvgProcessingHoursByPriority(byPriority);
        }

        return vo;
    }

    @Override
    public WorkOrderTrendRespVO getWorkOrderTrendAnalysis(WorkOrderTrendReqVO reqVO) {
        LocalDateTime endTime = reqVO.getEndTime() != null ? reqVO.getEndTime() : LocalDateTime.now();
        LocalDateTime startTime = reqVO.getStartTime() != null ? reqVO.getStartTime() : endTime.minusDays(30);

        List<WorkOrderDO> allOrders = workOrderMapper.selectAll();

        WorkOrderTrendRespVO vo = new WorkOrderTrendRespVO();
        List<WorkOrderTrendRespVO.TrendItem> dailyTrends = new ArrayList<>();

        LocalDate current = startTime.toLocalDate();
        LocalDate end = endTime.toLocalDate();
        while (!current.isAfter(end)) {
            LocalDate day = current;
            WorkOrderTrendRespVO.TrendItem item = new WorkOrderTrendRespVO.TrendItem();
            item.setDate(day.toString());

            item.setNewCount(allOrders.stream()
                    .filter(o -> o.getCreateTime() != null && o.getCreateTime().toLocalDate().equals(day))
                    .count());
            item.setProcessingCount(allOrders.stream()
                    .filter(o -> o.getHandleTime() != null && o.getHandleTime().toLocalDate().equals(day))
                    .count());
            item.setCompletedCount(allOrders.stream()
                    .filter(o -> o.getFinishTime() != null && o.getFinishTime().toLocalDate().equals(day))
                    .count());
            item.setReturnedCount(allOrders.stream()
                    .filter(o -> WorkOrderStatusEnum.RETURNED.getStatus().equals(o.getStatus())
                            && o.getUpdateTime() != null && o.getUpdateTime().toLocalDate().equals(day))
                    .count());

            dailyTrends.add(item);
            current = current.plusDays(1);
        }
        vo.setDailyTrends(dailyTrends);

        return vo;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteWorkOrder(Long id) {
        // 1. 校验存在
        WorkOrderDO workOrder = validateWorkOrderExists(id);
        // 2. 校验状态（处理中的工单不允许删除）
        if (WorkOrderStatusEnum.PROCESSING.getStatus().equals(workOrder.getStatus())) {
            throw exception(WORK_ORDER_DELETE_FAIL);
        }
        // 3. 删除
        workOrderMapper.deleteById(id);
    }

    private WorkOrderDO validateWorkOrderExists(Long id) {
        WorkOrderDO workOrder = workOrderMapper.selectById(id);
        if (workOrder == null) {
            throw exception(WORK_ORDER_NOT_EXISTS);
        }
        return workOrder;
    }

    // ======================= 查询相关 =======================

    @Override
    public WorkOrderDO getWorkOrder(Long id) {
        return workOrderMapper.selectById(id);
    }

    @Override
    public List<WorkOrderDO> getWorkOrderList(Collection<Long> ids) {
        if (CollUtil.isEmpty(ids)) {
            return ListUtil.empty();
        }
        return workOrderMapper.selectByIds(ids);
    }

    @Override
    public PageResult<WorkOrderDO> getWorkOrderPage(WorkOrderPageReqVO pageReqVO) {
        return workOrderMapper.selectPage(pageReqVO);
    }

    @Override
    public Long getWorkOrderCountByTypeId(Long typeId) {
        return workOrderMapper.selectCountByTypeId(typeId);
    }

}
