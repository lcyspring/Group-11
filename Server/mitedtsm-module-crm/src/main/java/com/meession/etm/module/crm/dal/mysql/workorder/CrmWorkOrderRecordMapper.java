package com.meession.etm.module.crm.dal.mysql.workorder;

import com.meession.etm.framework.mybatis.core.mapper.BaseMapperX;
import com.meession.etm.framework.mybatis.core.query.LambdaQueryWrapperX;
import com.meession.etm.module.crm.dal.dataobject.workorder.CrmWorkOrderRecordDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface CrmWorkOrderRecordMapper extends BaseMapperX<CrmWorkOrderRecordDO> {

    default List<CrmWorkOrderRecordDO> selectListByWorkOrderId(Long workOrderId) {
        return selectList(new LambdaQueryWrapperX<CrmWorkOrderRecordDO>()
                .eq(CrmWorkOrderRecordDO::getWorkOrderId, workOrderId)
                .orderByAsc(CrmWorkOrderRecordDO::getId));
    }

    default void deleteByWorkOrderId(Long workOrderId) {
        delete(CrmWorkOrderRecordDO::getWorkOrderId, workOrderId);
    }
}
