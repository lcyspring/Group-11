package com.meession.etm.module.crm.dal.mysql.clue;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface CrmClueOwnerCapacityGuardMapper {

    /**
     * Serializes all operations that may increase one user's active clue ownership. The single
     * upsert acquires an exclusive unique-key lock whether the guard is new or already exists;
     * splitting this into INSERT IGNORE plus SELECT FOR UPDATE can deadlock during lock upgrade.
     */
    default void lockOwnerCapacity(Long tenantId, Long userId) {
        acquireGuardLock(tenantId, userId);
    }

    @Insert("""
            INSERT INTO crm_clue_owner_capacity_guard
              (user_id, creator, create_time, updater, update_time, tenant_id)
            VALUES
              (#{userId}, #{userId}, NOW(), #{userId}, NOW(), #{tenantId})
            ON DUPLICATE KEY UPDATE id = id
            """)
    int acquireGuardLock(@Param("tenantId") Long tenantId, @Param("userId") Long userId);
}
