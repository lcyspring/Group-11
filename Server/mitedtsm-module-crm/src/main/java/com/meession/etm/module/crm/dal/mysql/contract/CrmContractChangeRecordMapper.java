package com.meession.etm.module.crm.dal.mysql.contract;

import com.meession.etm.framework.mybatis.core.mapper.BaseMapperX;
import com.meession.etm.framework.mybatis.core.query.LambdaQueryWrapperX;
import com.meession.etm.module.crm.dal.dataobject.contract.CrmContractChangeRecordDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

import static com.meession.etm.module.crm.enums.contract.CrmContractLifecycleEnums.ACTION_AMENDMENT_EFFECTIVE;

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

    /**
     * 查询当前已经生效的合同版本。补充协议的创建、编辑、提交、驳回和取消属于候选版本轨迹，
     * 不能提前推进合同的当前有效版本；只有补充协议审批通过动作会推进版本。
     */
    default CrmContractChangeRecordDO selectLatestEffective(Long contractId) {
        return selectOne(new LambdaQueryWrapperX<CrmContractChangeRecordDO>()
                .eq(CrmContractChangeRecordDO::getContractId, contractId)
                .and(wrapper -> wrapper.le(CrmContractChangeRecordDO::getActionType, 8)
                        .or().eq(CrmContractChangeRecordDO::getActionType, ACTION_AMENDMENT_EFFECTIVE))
                .orderByDesc(CrmContractChangeRecordDO::getSequenceNo)
                .last("LIMIT 1"));
    }
}
