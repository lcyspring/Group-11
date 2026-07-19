package com.meession.etm.module.crm.dal.mysql.customer;

import com.meession.etm.framework.mybatis.core.mapper.BaseMapperX;
import com.meession.etm.framework.mybatis.core.query.LambdaQueryWrapperX;
import com.meession.etm.module.crm.dal.dataobject.customer.CrmCustomerOwnerRecordDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;
import java.util.Collection;
import java.time.LocalDateTime;

import static com.meession.etm.module.crm.enums.customer.CrmCustomerOwnerRecordSourceEnum.SELF_CLAIM;
import static com.meession.etm.module.crm.enums.customer.CrmCustomerOwnerRecordTypeEnum.PUT_POOL;
import static com.meession.etm.module.crm.enums.customer.CrmCustomerOwnerRecordTypeEnum.TAKE_POOL;

/**
 * CRM 客户归属变更记录 Mapper。
 */
@Mapper
public interface CrmCustomerOwnerRecordMapper extends BaseMapperX<CrmCustomerOwnerRecordDO> {

    default List<CrmCustomerOwnerRecordDO> selectListByCustomerId(Long customerId) {
        return selectList(new LambdaQueryWrapperX<CrmCustomerOwnerRecordDO>()
                .eq(CrmCustomerOwnerRecordDO::getCustomerId, customerId)
                .orderByDesc(CrmCustomerOwnerRecordDO::getId));
    }

    default List<CrmCustomerOwnerRecordDO> selectLatestPoolRecords(Collection<Long> customerIds) {
        if (customerIds == null || customerIds.isEmpty()) {
            return List.of();
        }
        return selectList(new LambdaQueryWrapperX<CrmCustomerOwnerRecordDO>()
                .in(CrmCustomerOwnerRecordDO::getCustomerId, customerIds)
                .eq(CrmCustomerOwnerRecordDO::getType, PUT_POOL.getType())
                .orderByDesc(CrmCustomerOwnerRecordDO::getId));
    }

    default long selectTodaySelfClaimCount(Long ownerUserId, LocalDateTime start, LocalDateTime end) {
        return selectCount(new LambdaQueryWrapperX<CrmCustomerOwnerRecordDO>()
                .eq(CrmCustomerOwnerRecordDO::getType, TAKE_POOL.getType())
                .eq(CrmCustomerOwnerRecordDO::getSource, SELF_CLAIM.getSource())
                .eq(CrmCustomerOwnerRecordDO::getNewOwnerUserId, ownerUserId)
                .between(CrmCustomerOwnerRecordDO::getCreateTime, start, end));
    }

    default boolean existsRecentSelfClaim(Long customerId, Long ownerUserId, LocalDateTime since) {
        return selectCount(new LambdaQueryWrapperX<CrmCustomerOwnerRecordDO>()
                .eq(CrmCustomerOwnerRecordDO::getCustomerId, customerId)
                .eq(CrmCustomerOwnerRecordDO::getType, TAKE_POOL.getType())
                .eq(CrmCustomerOwnerRecordDO::getSource, SELF_CLAIM.getSource())
                .eq(CrmCustomerOwnerRecordDO::getNewOwnerUserId, ownerUserId)
                .ge(CrmCustomerOwnerRecordDO::getCreateTime, since)) > 0;
    }

}
