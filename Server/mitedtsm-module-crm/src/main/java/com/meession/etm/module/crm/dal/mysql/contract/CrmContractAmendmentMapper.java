package com.meession.etm.module.crm.dal.mysql.contract;

import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.meession.etm.framework.mybatis.core.mapper.BaseMapperX;
import com.meession.etm.framework.mybatis.core.query.LambdaQueryWrapperX;
import com.meession.etm.module.crm.dal.dataobject.contract.CrmContractAmendmentDO;
import com.meession.etm.module.crm.enums.common.CrmAuditStatusEnum;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface CrmContractAmendmentMapper extends BaseMapperX<CrmContractAmendmentDO> {

    default CrmContractAmendmentDO selectByIdForUpdate(Long id) {
        return selectOne(new LambdaQueryWrapperX<CrmContractAmendmentDO>()
                .eq(CrmContractAmendmentDO::getId, id).last("FOR UPDATE"));
    }

    default CrmContractAmendmentDO selectByRequestId(String requestId) {
        return selectOne(CrmContractAmendmentDO::getClientRequestId, requestId);
    }

    default List<CrmContractAmendmentDO> selectListByContractId(Long contractId) {
        return selectList(new LambdaQueryWrapperX<CrmContractAmendmentDO>()
                .eq(CrmContractAmendmentDO::getContractId, contractId)
                .orderByDesc(CrmContractAmendmentDO::getTargetVersion)
                .orderByDesc(CrmContractAmendmentDO::getId));
    }

    default CrmContractAmendmentDO selectOpenByContractId(Long contractId) {
        return selectOne(new LambdaQueryWrapperX<CrmContractAmendmentDO>()
                .eq(CrmContractAmendmentDO::getContractId, contractId)
                .in(CrmContractAmendmentDO::getAuditStatus,
                        CrmAuditStatusEnum.DRAFT.getStatus(), CrmAuditStatusEnum.PROCESS.getStatus(),
                        CrmAuditStatusEnum.REJECT.getStatus(), CrmAuditStatusEnum.CANCEL.getStatus())
                .orderByDesc(CrmContractAmendmentDO::getId).last("LIMIT 1"));
    }

    default int updateAuditStatusIfProcessing(Long id, String processInstanceId, Integer auditStatus,
                                               java.time.LocalDateTime effectiveTime) {
        return update(new LambdaUpdateWrapper<CrmContractAmendmentDO>()
                .eq(CrmContractAmendmentDO::getId, id)
                .eq(CrmContractAmendmentDO::getProcessInstanceId, processInstanceId)
                .eq(CrmContractAmendmentDO::getAuditStatus, CrmAuditStatusEnum.PROCESS.getStatus())
                .set(CrmContractAmendmentDO::getAuditStatus, auditStatus)
                .set(effectiveTime != null, CrmContractAmendmentDO::getEffectiveTime, effectiveTime));
    }
}
