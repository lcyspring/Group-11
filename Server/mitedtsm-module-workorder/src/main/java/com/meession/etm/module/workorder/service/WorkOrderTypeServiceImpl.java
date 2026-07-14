package com.meession.etm.module.workorder.service;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.collection.ListUtil;
import com.meession.etm.framework.common.pojo.PageResult;
import com.meession.etm.framework.common.util.object.BeanUtils;
import com.meession.etm.module.workorder.controller.admin.workorder.vo.type.WorkOrderTypePageReqVO;
import com.meession.etm.module.workorder.controller.admin.workorder.vo.type.WorkOrderTypeSaveReqVO;
import com.meession.etm.module.workorder.dal.dataobject.WorkOrderTypeDO;
import com.meession.etm.module.workorder.dal.mysql.WorkOrderTypeMapper;
import jakarta.annotation.Resource;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.util.Collection;
import java.util.List;

import static com.meession.etm.framework.common.exception.util.ServiceExceptionUtil.exception;
import static com.meession.etm.module.workorder.enums.ErrorCodeConstants.*;

/**
 * 工单类型 Service 实现类
 *
 * @author fwx
 */
@Service
@Validated
public class WorkOrderTypeServiceImpl implements WorkOrderTypeService {

    @Resource
    private WorkOrderTypeMapper workOrderTypeMapper;

    @Resource
    @Lazy // 延迟加载，避免循环依赖
    private WorkOrderService workOrderService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createWorkOrderType(WorkOrderTypeSaveReqVO createReqVO) {
        // 1. 校验编码和名称唯一性
        validateCodeUnique(createReqVO.getCode(), null);
        validateNameUnique(createReqVO.getName(), null);

        // 2. 插入
        WorkOrderTypeDO type = BeanUtils.toBean(createReqVO, WorkOrderTypeDO.class);
        if (type.getSort() == null) {
            type.setSort(0);
        }
        if (type.getStatus() == null) {
            type.setStatus(0); // 默认启用
        }
        workOrderTypeMapper.insert(type);
        return type.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateWorkOrderType(WorkOrderTypeSaveReqVO updateReqVO) {
        // 1. 校验存在
        validateWorkOrderTypeExists(updateReqVO.getId());

        // 2. 校验编码和名称唯一性
        validateCodeUnique(updateReqVO.getCode(), updateReqVO.getId());
        validateNameUnique(updateReqVO.getName(), updateReqVO.getId());

        // 3. 更新
        WorkOrderTypeDO updateObj = BeanUtils.toBean(updateReqVO, WorkOrderTypeDO.class);
        workOrderTypeMapper.updateById(updateObj);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteWorkOrderType(Long id) {
        // 1. 校验存在
        validateWorkOrderTypeExists(id);
        // 2. 校验是否被工单使用
        Long count = workOrderService.getWorkOrderCountByTypeId(id);
        if (count > 0) {
            throw exception(WORK_ORDER_TYPE_USED);
        }
        // 3. 删除
        workOrderTypeMapper.deleteById(id);
    }

    private void validateCodeUnique(String code, Long excludeId) {
        WorkOrderTypeDO existing = workOrderTypeMapper.selectByCode(code);
        if (existing != null && !existing.getId().equals(excludeId)) {
            throw exception(WORK_ORDER_TYPE_CODE_EXISTS);
        }
    }

    private void validateNameUnique(String name, Long excludeId) {
        WorkOrderTypeDO existing = workOrderTypeMapper.selectByName(name);
        if (existing != null && !existing.getId().equals(excludeId)) {
            throw exception(WORK_ORDER_TYPE_NAME_EXISTS);
        }
    }

    @Override
    public void validateWorkOrderTypeExists(Long id) {
        WorkOrderTypeDO type = workOrderTypeMapper.selectById(id);
        if (type == null) {
            throw exception(WORK_ORDER_TYPE_NOT_EXISTS);
        }
    }

    // ======================= 查询相关 =======================

    @Override
    public WorkOrderTypeDO getWorkOrderType(Long id) {
        return workOrderTypeMapper.selectById(id);
    }

    @Override
    public List<WorkOrderTypeDO> getWorkOrderTypeList(Collection<Long> ids) {
        if (CollUtil.isEmpty(ids)) {
            return ListUtil.empty();
        }
        return workOrderTypeMapper.selectByIds(ids);
    }

    @Override
    public List<WorkOrderTypeDO> getEnableWorkOrderTypeList() {
        return workOrderTypeMapper.selectList(WorkOrderTypeDO::getStatus, 0);
    }

    @Override
    public PageResult<WorkOrderTypeDO> getWorkOrderTypePage(WorkOrderTypePageReqVO pageReqVO) {
        return workOrderTypeMapper.selectPage(pageReqVO);
    }

}
