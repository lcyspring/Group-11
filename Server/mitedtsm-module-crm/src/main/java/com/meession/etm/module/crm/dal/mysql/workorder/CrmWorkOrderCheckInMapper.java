package com.meession.etm.module.crm.dal.mysql.workorder;

import com.meession.etm.framework.mybatis.core.mapper.BaseMapperX;
import com.meession.etm.framework.mybatis.core.query.LambdaQueryWrapperX;
import com.meession.etm.module.crm.dal.dataobject.workorder.CrmWorkOrderCheckInDO;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface CrmWorkOrderCheckInMapper extends BaseMapperX<CrmWorkOrderCheckInDO> {
    default CrmWorkOrderCheckInDO selectLatest(Long workOrderId) {
        return selectOne(new LambdaQueryWrapperX<CrmWorkOrderCheckInDO>()
                .eq(CrmWorkOrderCheckInDO::getWorkOrderId, workOrderId)
                .orderByDesc(CrmWorkOrderCheckInDO::getCreateTime)
                .last("LIMIT 1"));
    }
    default boolean exists(Long workOrderId) {
        return selectCount(new LambdaQueryWrapperX<CrmWorkOrderCheckInDO>()
                .eq(CrmWorkOrderCheckInDO::getWorkOrderId, workOrderId)) > 0;
    }
}
