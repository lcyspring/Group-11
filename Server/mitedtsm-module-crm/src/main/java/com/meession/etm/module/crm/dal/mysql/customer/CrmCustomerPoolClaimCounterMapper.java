package com.meession.etm.module.crm.dal.mysql.customer;

import com.meession.etm.framework.mybatis.core.mapper.BaseMapperX;
import com.meession.etm.module.crm.dal.dataobject.customer.CrmCustomerPoolClaimCounterDO;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

import java.time.LocalDate;

@Mapper
public interface CrmCustomerPoolClaimCounterMapper extends BaseMapperX<CrmCustomerPoolClaimCounterDO> {

    /**
     * Atomically reserves quota without depending on Connector/J affected-row compatibility flags.
     * A concurrent first insert is handled by retrying the guarded update after INSERT IGNORE returns.
     */
    default int reserve(Long tenantId, Long userId, LocalDate claimDate, int increment, int claimLimit) {
        if (increment <= 0 || increment > claimLimit) {
            return 0;
        }
        if (incrementExisting(tenantId, userId, claimDate, increment, claimLimit) > 0) {
            return 1;
        }
        if (insertInitial(tenantId, userId, claimDate, increment) > 0) {
            return 1;
        }
        return incrementExisting(tenantId, userId, claimDate, increment, claimLimit) > 0 ? 1 : 0;
    }

    @Update("""
            UPDATE crm_customer_pool_claim_counter
            SET claim_count = claim_count + #{increment},
                updater = #{userId}, update_time = NOW()
            WHERE tenant_id = #{tenantId} AND user_id = #{userId} AND claim_date = #{claimDate}
              AND deleted = b'0' AND claim_count + #{increment} <= #{claimLimit}
            """)
    int incrementExisting(@Param("tenantId") Long tenantId, @Param("userId") Long userId,
                          @Param("claimDate") LocalDate claimDate, @Param("increment") int increment,
                          @Param("claimLimit") int claimLimit);

    @Insert("""
            INSERT IGNORE INTO crm_customer_pool_claim_counter
              (user_id, claim_date, claim_count, creator, create_time, updater, update_time, deleted, tenant_id)
            VALUES
              (#{userId}, #{claimDate}, #{increment}, #{userId}, NOW(), #{userId}, NOW(), b'0', #{tenantId})
            """)
    int insertInitial(@Param("tenantId") Long tenantId, @Param("userId") Long userId,
                      @Param("claimDate") LocalDate claimDate, @Param("increment") int increment);
}
