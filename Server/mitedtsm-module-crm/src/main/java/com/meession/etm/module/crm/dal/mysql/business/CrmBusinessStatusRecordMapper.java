package com.meession.etm.module.crm.dal.mysql.business;

import com.meession.etm.framework.mybatis.core.mapper.BaseMapperX;
import com.meession.etm.module.crm.dal.dataobject.business.CrmBusinessStatusRecordDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface CrmBusinessStatusRecordMapper extends BaseMapperX<CrmBusinessStatusRecordDO> {

    default List<CrmBusinessStatusRecordDO> selectListByBusinessId(Long businessId) {
        return selectList(CrmBusinessStatusRecordDO::getBusinessId, businessId);
    }

}