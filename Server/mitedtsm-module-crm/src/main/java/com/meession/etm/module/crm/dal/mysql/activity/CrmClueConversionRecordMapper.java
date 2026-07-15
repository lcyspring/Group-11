package com.meession.etm.module.crm.dal.mysql.activity;

import com.meession.etm.framework.mybatis.core.mapper.BaseMapperX;
import com.meession.etm.framework.mybatis.core.query.LambdaQueryWrapperX;
import com.meession.etm.module.crm.dal.dataobject.activity.CrmClueConversionRecordDO;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface CrmClueConversionRecordMapper extends BaseMapperX<CrmClueConversionRecordDO> {
    default CrmClueConversionRecordDO selectByClueId(Long clueId) {
        return selectOne(CrmClueConversionRecordDO::getClueId, clueId);
    }

    default CrmClueConversionRecordDO selectByCustomerId(Long customerId) {
        return selectOne(new LambdaQueryWrapperX<CrmClueConversionRecordDO>()
                .eq(CrmClueConversionRecordDO::getCustomerId, customerId));
    }
}
