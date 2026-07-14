package com.meession.etm.module.workorder.service;

import com.meession.etm.framework.common.pojo.PageResult;
import com.meession.etm.module.workorder.controller.admin.workorder.vo.type.WorkOrderTypePageReqVO;
import com.meession.etm.module.workorder.controller.admin.workorder.vo.type.WorkOrderTypeSaveReqVO;
import com.meession.etm.module.workorder.dal.dataobject.WorkOrderTypeDO;
import jakarta.validation.Valid;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import static com.meession.etm.framework.common.util.collection.CollectionUtils.convertMap;

/**
 * 工单类型 Service 接口
 *
 * @author fwx
 */
public interface WorkOrderTypeService {

    /**
     * 创建工单类型
     *
     * @param createReqVO 创建信息
     * @return 编号
     */
    Long createWorkOrderType(@Valid WorkOrderTypeSaveReqVO createReqVO);

    /**
     * 更新工单类型
     *
     * @param updateReqVO 更新信息
     */
    void updateWorkOrderType(@Valid WorkOrderTypeSaveReqVO updateReqVO);

    /**
     * 删除工单类型
     *
     * @param id 编号
     */
    void deleteWorkOrderType(Long id);

    /**
     * 获得工单类型
     *
     * @param id 编号
     * @return 工单类型
     */
    WorkOrderTypeDO getWorkOrderType(Long id);

    /**
     * 获得工单类型列表
     *
     * @param ids 编号数组
     * @return 工单类型列表
     */
    List<WorkOrderTypeDO> getWorkOrderTypeList(Collection<Long> ids);

    /**
     * 获得工单类型 Map
     *
     * @param ids 编号数组
     * @return 工单类型 Map
     */
    default Map<Long, WorkOrderTypeDO> getWorkOrderTypeMap(Collection<Long> ids) {
        return convertMap(getWorkOrderTypeList(ids), WorkOrderTypeDO::getId);
    }

    /**
     * 获得所有启用的工单类型列表
     *
     * @return 工单类型列表
     */
    List<WorkOrderTypeDO> getEnableWorkOrderTypeList();

    /**
     * 获得工单类型分页
     *
     * @param pageReqVO 分页查询
     * @return 工单类型分页
     */
    PageResult<WorkOrderTypeDO> getWorkOrderTypePage(WorkOrderTypePageReqVO pageReqVO);

    /**
     * 校验工单类型是否存在
     *
     * @param id 编号
     */
    void validateWorkOrderTypeExists(Long id);

}
