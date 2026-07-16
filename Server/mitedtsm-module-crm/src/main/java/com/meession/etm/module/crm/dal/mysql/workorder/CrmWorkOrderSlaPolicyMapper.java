package com.meession.etm.module.crm.dal.mysql.workorder;

import com.meession.etm.framework.mybatis.core.mapper.BaseMapperX;
import com.meession.etm.framework.mybatis.core.query.LambdaQueryWrapperX;
import com.meession.etm.module.crm.dal.dataobject.workorder.CrmWorkOrderSlaPolicyDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface CrmWorkOrderSlaPolicyMapper extends BaseMapperX<CrmWorkOrderSlaPolicyDO> {
    default CrmWorkOrderSlaPolicyDO selectByCode(String code) {
        return selectOne(new LambdaQueryWrapperX<CrmWorkOrderSlaPolicyDO>()
                .eq(CrmWorkOrderSlaPolicyDO::getCode, code)
                .eq(CrmWorkOrderSlaPolicyDO::getEnabled, true));
    }
    default List<CrmWorkOrderSlaPolicyDO> selectEnabled() {
        return selectList(new LambdaQueryWrapperX<CrmWorkOrderSlaPolicyDO>()
                .eq(CrmWorkOrderSlaPolicyDO::getEnabled, true)
                .orderByAsc(CrmWorkOrderSlaPolicyDO::getSort)
                .orderByAsc(CrmWorkOrderSlaPolicyDO::getId));
    }
}
