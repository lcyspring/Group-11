package com.meession.etm.module.bpm.dal.mysql.oa;

import com.meession.etm.framework.mybatis.core.mapper.BaseMapperX;
import com.meession.etm.module.bpm.dal.dataobject.oa.BpmOALeaveBalanceDO;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface BpmOALeaveBalanceMapper extends BaseMapperX<BpmOALeaveBalanceDO> {
    @Insert("INSERT IGNORE INTO bpm_oa_leave_balance(user_id,leave_type,balance_year,total_days,reserved_days,used_days,creator,create_time,updater,update_time,deleted,tenant_id) " +
            "VALUES(#{userId},#{leaveType},#{year},#{totalDays},0,0,'oa-leave',NOW(),'oa-leave',NOW(),b'0',#{tenantId})")
    int insertDefault(@Param("userId") Long userId, @Param("leaveType") Integer leaveType,
                      @Param("year") Integer year, @Param("totalDays") Long totalDays,
                      @Param("tenantId") Long tenantId);

    @Select("SELECT * FROM bpm_oa_leave_balance WHERE user_id=#{userId} AND leave_type=#{leaveType} " +
            "AND balance_year=#{year} AND tenant_id=#{tenantId} AND deleted=b'0' FOR UPDATE")
    BpmOALeaveBalanceDO selectForUpdate(@Param("userId") Long userId, @Param("leaveType") Integer leaveType,
                                        @Param("year") Integer year, @Param("tenantId") Long tenantId);
}
