package com.meession.etm.module.crm.dal.mysql.refund;

import com.meession.etm.framework.mybatis.core.mapper.BaseMapperX;
import com.meession.etm.framework.mybatis.core.query.LambdaQueryWrapperX;
import com.meession.etm.module.crm.dal.dataobject.refund.CrmReceivableRefundActionRecordDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface CrmReceivableRefundActionRecordMapper
        extends BaseMapperX<CrmReceivableRefundActionRecordDO> {

    default List<CrmReceivableRefundActionRecordDO> selectListByRefundId(Long refundId) {
        return selectList(new LambdaQueryWrapperX<CrmReceivableRefundActionRecordDO>()
                .eq(CrmReceivableRefundActionRecordDO::getRefundId, refundId)
                .orderByAsc(CrmReceivableRefundActionRecordDO::getId));
    }
}
