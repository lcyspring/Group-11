package com.meession.etm.module.crm.dal.mysql.customer;

import com.meession.etm.framework.mybatis.core.mapper.BaseMapperX;
import com.meession.etm.framework.mybatis.core.query.LambdaQueryWrapperX;
import com.meession.etm.module.crm.dal.dataobject.customer.CrmCustomerConfigDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface CrmCustomerConfigMapper extends BaseMapperX<CrmCustomerConfigDO> {

    default List<CrmCustomerConfigDO> selectListByConfigType(String configType) {
        return selectList(new LambdaQueryWrapperX<CrmCustomerConfigDO>()
                .eq(CrmCustomerConfigDO::getConfigType, configType)
                .eq(CrmCustomerConfigDO::getStatus, true)
                .orderByAsc(CrmCustomerConfigDO::getSort));
    }

    default List<CrmCustomerConfigDO> selectListByConfigTypeIncludeDisabled(String configType) {
        return selectList(new LambdaQueryWrapperX<CrmCustomerConfigDO>()
                .eq(CrmCustomerConfigDO::getConfigType, configType)
                .orderByAsc(CrmCustomerConfigDO::getSort));
    }

    default CrmCustomerConfigDO selectByConfigTypeAndValue(String configType, Integer configValue) {
        return selectOne(new LambdaQueryWrapperX<CrmCustomerConfigDO>()
                .eq(CrmCustomerConfigDO::getConfigType, configType)
                .eq(CrmCustomerConfigDO::getConfigValue, configValue));
    }

}