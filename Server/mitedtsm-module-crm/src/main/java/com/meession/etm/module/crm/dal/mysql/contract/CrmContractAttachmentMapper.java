package com.meession.etm.module.crm.dal.mysql.contract;

import com.meession.etm.framework.mybatis.core.mapper.BaseMapperX;
import com.meession.etm.framework.mybatis.core.query.LambdaQueryWrapperX;
import com.meession.etm.module.crm.dal.dataobject.contract.CrmContractAttachmentDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface CrmContractAttachmentMapper extends BaseMapperX<CrmContractAttachmentDO> {
    default List<CrmContractAttachmentDO> selectListByContractId(Long contractId) {
        return selectList(new LambdaQueryWrapperX<CrmContractAttachmentDO>()
                .eq(CrmContractAttachmentDO::getContractId, contractId)
                .orderByDesc(CrmContractAttachmentDO::getId));
    }

    default List<CrmContractAttachmentDO> selectListByAmendmentId(Long amendmentId) {
        return selectList(new LambdaQueryWrapperX<CrmContractAttachmentDO>()
                .eq(CrmContractAttachmentDO::getAmendmentId, amendmentId)
                .orderByAsc(CrmContractAttachmentDO::getId));
    }
}
