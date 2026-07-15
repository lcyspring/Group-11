package com.meession.etm.module.crm.dal.mysql.clue;

import com.meession.etm.framework.mybatis.core.mapper.BaseMapperX;
import com.meession.etm.framework.mybatis.core.query.LambdaQueryWrapperX;
import com.meession.etm.module.crm.dal.dataobject.clue.CrmClueOwnerRecordDO;
import com.meession.etm.module.crm.enums.clue.CrmClueOwnerRecordSourceEnum;
import org.apache.ibatis.annotations.Mapper;

import java.time.LocalDateTime;

@Mapper
public interface CrmClueOwnerRecordMapper extends BaseMapperX<CrmClueOwnerRecordDO> {

    default boolean existsRecentSelfClaim(Long clueId, Long ownerUserId, LocalDateTime since) {
        return selectCount(new LambdaQueryWrapperX<CrmClueOwnerRecordDO>()
                .eq(CrmClueOwnerRecordDO::getClueId, clueId)
                .eq(CrmClueOwnerRecordDO::getNewOwnerUserId, ownerUserId)
                .eq(CrmClueOwnerRecordDO::getSource, CrmClueOwnerRecordSourceEnum.SELF_CLAIM.getSource())
                .ge(CrmClueOwnerRecordDO::getCreateTime, since)) > 0;
    }
}
