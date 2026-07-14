package com.meession.etm.module.crm.dal.mysql.contract;

import com.meession.etm.framework.mybatis.core.mapper.BaseMapperX;
import com.meession.etm.framework.mybatis.core.query.LambdaQueryWrapperX;
import com.meession.etm.module.crm.dal.dataobject.contract.CrmContractSigningDO;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface CrmContractSigningMapper extends BaseMapperX<CrmContractSigningDO> {

    default CrmContractSigningDO selectByContractId(Long contractId) {
        return selectOne(new LambdaQueryWrapperX<CrmContractSigningDO>()
                .eq(CrmContractSigningDO::getContractId, contractId));
    }
}
