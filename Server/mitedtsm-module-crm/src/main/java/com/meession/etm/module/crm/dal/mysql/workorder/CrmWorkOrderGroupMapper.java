package com.meession.etm.module.crm.dal.mysql.workorder;

import com.meession.etm.framework.mybatis.core.mapper.BaseMapperX;
import com.meession.etm.framework.mybatis.core.query.LambdaQueryWrapperX;
import com.meession.etm.module.crm.dal.dataobject.workorder.CrmWorkOrderGroupDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface CrmWorkOrderGroupMapper extends BaseMapperX<CrmWorkOrderGroupDO> {

    default CrmWorkOrderGroupDO selectByCode(String code) {
        return selectOne(CrmWorkOrderGroupDO::getCode, code);
    }

    default List<CrmWorkOrderGroupDO> selectListOrdered() {
        return selectList(new LambdaQueryWrapperX<CrmWorkOrderGroupDO>()
                .orderByAsc(CrmWorkOrderGroupDO::getSort).orderByAsc(CrmWorkOrderGroupDO::getId));
    }
}
