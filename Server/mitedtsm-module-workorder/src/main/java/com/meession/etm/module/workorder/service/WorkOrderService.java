package com.meession.etm.module.workorder.service;

import com.meession.etm.framework.common.pojo.PageResult;
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
import jakarta.validation.Valid;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import static com.meession.etm.framework.common.util.collection.CollectionUtils.convertMap;

/**
 * 工单 Service 接口
 *
 * @author fwx
 */
public interface WorkOrderService {

    /**
     * 创建工单
     *
     * @param createReqVO 创建信息
     * @param userId      用户编号
     * @return 编号
     */
    Long createWorkOrder(@Valid WorkOrderSaveReqVO createReqVO, Long userId);

    /**
     * 更新工单
     *
     * @param updateReqVO 更新信息
     */
    void updateWorkOrder(@Valid WorkOrderSaveReqVO updateReqVO);

    /**
     * 更新工单状态
     *
     * @param reqVO 更新状态请求
     */
    void updateWorkOrderStatus(WorkOrderUpdateStatusReqVO reqVO);

    /**
     * 更新工单优先级
     *
     * @param reqVO 更新优先级请求
     */
    void updateWorkOrderPriority(WorkOrderUpdatePriorityReqVO reqVO);

    /**
     * 分配工单
     *
     * @param reqVO 分配请求
     */
    void assignWorkOrder(WorkOrderAssignReqVO reqVO);

    /**
     * 处理工单（开始处理）
     *
     * @param reqVO 处理请求
     * @param userId 当前用户编号
     */
    void processWorkOrder(WorkOrderProcessReqVO reqVO, Long userId);

    /**
     * 完结工单
     *
     * @param reqVO 完结请求
     */
    void completeWorkOrder(WorkOrderCompleteReqVO reqVO);

    /**
     * 退回工单
     *
     * @param reqVO 退回请求
     */
    void returnWorkOrder(WorkOrderReturnReqVO reqVO);

    /**
     * 获取工单统计
     *
     * @return 统计数据
     */
    WorkOrderStatisticsRespVO getWorkOrderStatistics();

    /**
     * 获取工单效率分析
     *
     * @return 效率分析数据
     */
    WorkOrderEfficiencyRespVO getWorkOrderEfficiencyAnalysis();

    /**
     * 获取工单趋势分析
     *
     * @param reqVO 趋势分析请求
     * @return 趋势数据
     */
    WorkOrderTrendRespVO getWorkOrderTrendAnalysis(WorkOrderTrendReqVO reqVO);

    /**
     * 删除工单
     *
     * @param id 编号
     */
    void deleteWorkOrder(Long id);

    /**
     * 获得工单
     *
     * @param id 编号
     * @return 工单
     */
    WorkOrderDO getWorkOrder(Long id);

    /**
     * 获得工单列表
     *
     * @param ids 编号数组
     * @return 工单列表
     */
    List<WorkOrderDO> getWorkOrderList(Collection<Long> ids);

    /**
     * 获得工单 Map
     *
     * @param ids 编号数组
     * @return 工单 Map
     */
    default Map<Long, WorkOrderDO> getWorkOrderMap(Collection<Long> ids) {
        return convertMap(getWorkOrderList(ids), WorkOrderDO::getId);
    }

    /**
     * 获得工单分页
     *
     * @param pageReqVO 分页查询
     * @return 工单分页
     */
    PageResult<WorkOrderDO> getWorkOrderPage(WorkOrderPageReqVO pageReqVO);

    /**
     * 获取指定工单类型的工单数量
     *
     * @param typeId 工单类型编号
     * @return 数量
     */
    Long getWorkOrderCountByTypeId(Long typeId);

}
