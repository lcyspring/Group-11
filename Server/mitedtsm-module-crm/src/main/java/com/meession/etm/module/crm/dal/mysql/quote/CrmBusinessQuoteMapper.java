package com.meession.etm.module.crm.dal.mysql.quote;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.meession.etm.framework.mybatis.core.mapper.BaseMapperX;
import com.meession.etm.module.crm.dal.dataobject.quote.CrmBusinessQuoteDO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface CrmBusinessQuoteMapper extends BaseMapperX<CrmBusinessQuoteDO> {
    default CrmBusinessQuoteDO selectLatest(Long businessId) {
        return selectOne(new LambdaQueryWrapper<CrmBusinessQuoteDO>()
                .eq(CrmBusinessQuoteDO::getBusinessId, businessId)
                .orderByDesc(CrmBusinessQuoteDO::getVersionNo).last("LIMIT 1"));
    }

    @Select("SELECT * FROM crm_business_quote WHERE business_id=#{businessId} AND deleted=0 "
            + "ORDER BY version_no DESC LIMIT 1 FOR UPDATE")
    CrmBusinessQuoteDO selectLatestForUpdate(@Param("businessId") Long businessId);

    default List<CrmBusinessQuoteDO> selectVersions(Long businessId) {
        return selectList(new LambdaQueryWrapper<CrmBusinessQuoteDO>()
                .eq(CrmBusinessQuoteDO::getBusinessId, businessId)
                .orderByDesc(CrmBusinessQuoteDO::getVersionNo));
    }

    default int lockDraft(Long id, Integer version, Long userId, LocalDateTime now) {
        return update(new LambdaUpdateWrapper<CrmBusinessQuoteDO>()
                .eq(CrmBusinessQuoteDO::getId, id)
                .eq(CrmBusinessQuoteDO::getVersion, version)
                .eq(CrmBusinessQuoteDO::getStatus, 0)
                .set(CrmBusinessQuoteDO::getStatus, 10)
                .set(CrmBusinessQuoteDO::getLockedBy, userId)
                .set(CrmBusinessQuoteDO::getLockedAt, now)
                .set(CrmBusinessQuoteDO::getVersion, version + 1));
    }

    default int supersedeLocked(Long id, Integer version) {
        return update(new LambdaUpdateWrapper<CrmBusinessQuoteDO>()
                .eq(CrmBusinessQuoteDO::getId, id)
                .eq(CrmBusinessQuoteDO::getVersion, version)
                .eq(CrmBusinessQuoteDO::getStatus, 10)
                .set(CrmBusinessQuoteDO::getStatus, 20)
                .set(CrmBusinessQuoteDO::getVersion, version + 1));
    }

    default int terminateCurrent(Long id, Integer version, Integer currentStatus) {
        return update(new LambdaUpdateWrapper<CrmBusinessQuoteDO>()
                .eq(CrmBusinessQuoteDO::getId, id)
                .eq(CrmBusinessQuoteDO::getVersion, version)
                .eq(CrmBusinessQuoteDO::getStatus, currentStatus)
                .in(CrmBusinessQuoteDO::getStatus, 0, 10)
                .set(CrmBusinessQuoteDO::getStatus, 30)
                .set(CrmBusinessQuoteDO::getVersion, version + 1));
    }
}
