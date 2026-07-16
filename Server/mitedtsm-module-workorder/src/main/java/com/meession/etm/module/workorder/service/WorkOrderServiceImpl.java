package com.meession.etm.module.workorder.service;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.collection.ListUtil;
import com.meession.etm.framework.common.pojo.PageResult;
import com.meession.etm.framework.common.util.object.BeanUtils;
import com.meession.etm.module.workorder.controller.admin.workorder.vo.workorder.WorkOrderAssignReqVO;
import com.meession.etm.module.workorder.controller.admin.workorder.vo.workorder.WorkOrderCompleteReqVO;
import com.meession.etm.module.workorder.controller.admin.workorder.vo.workorder.WorkOrderPageReqVO;
import com.meession.etm.module.workorder.controller.admin.workorder.vo.workorder.WorkOrderProcessReqVO;
import com.meession.etm.module.workorder.controller.admin.workorder.vo.workorder.WorkOrderSaveReqVO;
import com.meession.etm.module.workorder.controller.admin.workorder.vo.workorder.WorkOrderUpdatePriorityReqVO;
import com.meession.etm.module.workorder.controller.admin.workorder.vo.workorder.WorkOrderUpdateStatusReqVO;
import com.meession.etm.module.workorder.dal.dataobject.WorkOrderDO;
import com.meession.etm.module.workorder.dal.mysql.WorkOrderMapper;
import com.meession.etm.module.workorder.enums.WorkOrderStatusEnum;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

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
