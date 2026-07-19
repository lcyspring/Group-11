package com.meession.etm.module.crm.dal.mysql.workorder;

import com.meession.etm.framework.mybatis.core.mapper.BaseMapperX;
import com.meession.etm.framework.mybatis.core.query.LambdaQueryWrapperX;
import com.meession.etm.module.crm.dal.dataobject.workorder.CrmWorkOrderCcDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

@Mapper
public interface CrmWorkOrderCcMapper extends BaseMapperX<CrmWorkOrderCcDO> {

    default List<CrmWorkOrderCcDO> selectByWorkOrderIds(Collection<Long> workOrderIds) {
        if (workOrderIds == null || workOrderIds.isEmpty()) return Collections.emptyList();
        return selectList(new LambdaQueryWrapperX<CrmWorkOrderCcDO>()
                .in(CrmWorkOrderCcDO::getWorkOrderId, workOrderIds).orderByAsc(CrmWorkOrderCcDO::getId));
    }

    default List<CrmWorkOrderCcDO> selectByUserId(Long userId) {
        return selectList(CrmWorkOrderCcDO::getUserId, userId);
    }

    default boolean exists(Long workOrderId, Long userId) {
        return selectCount(new LambdaQueryWrapperX<CrmWorkOrderCcDO>()
                .eq(CrmWorkOrderCcDO::getWorkOrderId, workOrderId)
                .eq(CrmWorkOrderCcDO::getUserId, userId)) > 0;
    }

    default void deleteByWorkOrderId(Long workOrderId) {
        delete(CrmWorkOrderCcDO::getWorkOrderId, workOrderId);
    }
}
