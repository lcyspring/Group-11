package com.meession.etm.module.workorder.dal.mysql;

import com.meession.etm.framework.common.pojo.PageResult;
import com.meession.etm.framework.mybatis.core.mapper.BaseMapperX;
import com.meession.etm.framework.mybatis.core.query.LambdaQueryWrapperX;
import com.meession.etm.module.workorder.controller.admin.workorder.vo.workorder.WorkOrderPageReqVO;
import com.meession.etm.module.workorder.dal.dataobject.WorkOrderDO;
import org.apache.ibatis.annotations.Mapper;

/**
 * 工单 Mapper
 *
 * @author fwx
 */
@Mapper
public interface WorkOrderMapper extends BaseMapperX<WorkOrderDO> {

    default PageResult<WorkOrderDO> selectPage(WorkOrderPageReqVO pageReqVO) {
        return selectPage(pageReqVO, new LambdaQueryWrapperX<WorkOrderDO>()
                .likeIfPresent(WorkOrderDO::getTitle, pageReqVO.getTitle())
                .eqIfPresent(WorkOrderDO::getTypeId, pageReqVO.getTypeId())
                .eqIfPresent(WorkOrderDO::getPriority, pageReqVO.getPriority())
                .eqIfPresent(WorkOrderDO::getStatus, pageReqVO.getStatus())
                .eqIfPresent(WorkOrderDO::getHandlerUserId, pageReqVO.getHandlerUserId())
                .eqIfPresent(WorkOrderDO::getSubmitterUserId, pageReqVO.getSubmitterUserId())
                .orderByDesc(WorkOrderDO::getId));
    }

    default Long selectCountByTypeId(Long typeId) {
        return selectCount(WorkOrderDO::getTypeId, typeId);
    }

    default Long selectCountByStatus(Integer status) {
        return selectCount(WorkOrderDO::getStatus, status);
    }

}
