package com.meession.etm.module.crm.dal.mysql.fulfillment;

import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.meession.etm.framework.mybatis.core.mapper.BaseMapperX;
import com.meession.etm.framework.mybatis.core.query.LambdaQueryWrapperX;
import com.meession.etm.module.crm.dal.dataobject.fulfillment.CrmContractFulfillmentDO;
import org.apache.ibatis.annotations.Mapper;

import java.time.LocalDateTime;

import static com.meession.etm.module.crm.enums.fulfillment.CrmContractFulfillmentStatus.CREATING;

@Mapper
public interface CrmContractFulfillmentMapper extends BaseMapperX<CrmContractFulfillmentDO> {

    default CrmContractFulfillmentDO selectByContractId(Long contractId) {
        return selectOne(CrmContractFulfillmentDO::getContractId, contractId);
    }

    default CrmContractFulfillmentDO selectByContractIdForUpdate(Long contractId) {
        return selectOne(new LambdaQueryWrapperX<CrmContractFulfillmentDO>()
                .eq(CrmContractFulfillmentDO::getContractId, contractId).last("FOR UPDATE"));
    }

    default CrmContractFulfillmentDO selectByErpOrderId(Long erpOrderId) {
        return selectOne(CrmContractFulfillmentDO::getErpOrderId, erpOrderId);
    }

    default int markRetrying(Long id, int attemptCount, LocalDateTime now) {
        return update(new LambdaUpdateWrapper<CrmContractFulfillmentDO>()
                .eq(CrmContractFulfillmentDO::getId, id)
                .set(CrmContractFulfillmentDO::getStatus, CREATING)
                .set(CrmContractFulfillmentDO::getAttemptCount, attemptCount)
                .set(CrmContractFulfillmentDO::getLastAttemptTime, now)
                .set(CrmContractFulfillmentDO::getLastErrorCode, null)
                .set(CrmContractFulfillmentDO::getLastErrorMessage, null));
    }
}
