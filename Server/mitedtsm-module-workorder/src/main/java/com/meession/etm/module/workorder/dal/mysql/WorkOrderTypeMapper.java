package com.meession.etm.module.workorder.dal.mysql;

import com.meession.etm.framework.common.pojo.PageResult;
import com.meession.etm.framework.mybatis.core.mapper.BaseMapperX;
import com.meession.etm.framework.mybatis.core.query.LambdaQueryWrapperX;
import com.meession.etm.module.workorder.controller.admin.workorder.vo.type.WorkOrderTypePageReqVO;
import com.meession.etm.module.workorder.dal.dataobject.WorkOrderTypeDO;
import org.apache.ibatis.annotations.Mapper;

/**
 * 工单类型 Mapper
 *
 * @author fwx
 */
@Mapper
public interface WorkOrderTypeMapper extends BaseMapperX<WorkOrderTypeDO> {

    default PageResult<WorkOrderTypeDO> selectPage(WorkOrderTypePageReqVO pageReqVO) {
        return selectPage(pageReqVO, new LambdaQueryWrapperX<WorkOrderTypeDO>()
                .likeIfPresent(WorkOrderTypeDO::getName, pageReqVO.getName())
                .likeIfPresent(WorkOrderTypeDO::getCode, pageReqVO.getCode())
                .eqIfPresent(WorkOrderTypeDO::getStatus, pageReqVO.getStatus())
                .orderByAsc(WorkOrderTypeDO::getSort));
    }

    default WorkOrderTypeDO selectByCode(String code) {
        return selectOne(WorkOrderTypeDO::getCode, code);
    }

    default WorkOrderTypeDO selectByName(String name) {
        return selectOne(WorkOrderTypeDO::getName, name);
    }

}
