package com.meession.etm.module.crm.dal.mysql.contract;

import com.meession.etm.framework.mybatis.core.mapper.BaseMapperX;
import com.meession.etm.framework.mybatis.core.query.LambdaQueryWrapperX;
import com.meession.etm.module.crm.dal.dataobject.contract.CrmContractChangeRecordDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface CrmContractChangeRecordMapper extends BaseMapperX<CrmContractChangeRecordDO> {
    default List<CrmContractChangeRecordDO> selectListByContractId(Long contractId) {
        return selectList(new LambdaQueryWrapperX<CrmContractChangeRecordDO>()
                .eq(CrmContractChangeRecordDO::getContractId, contractId)
                .orderByDesc(CrmContractChangeRecordDO::getSequenceNo));
    }

    default CrmContractChangeRecordDO selectLatest(Long contractId) {
        return selectOne(new LambdaQueryWrapperX<CrmContractChangeRecordDO>()
                .eq(CrmContractChangeRecordDO::getContractId, contractId)
                .orderByDesc(CrmContractChangeRecordDO::getSequenceNo)
                .last("LIMIT 1"));
    }
}
