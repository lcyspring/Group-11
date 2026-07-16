package com.meession.etm.module.crm.dal.mysql.fulfillment;

import com.meession.etm.framework.common.pojo.PageParam;
import com.meession.etm.framework.common.pojo.PageResult;
import com.meession.etm.framework.mybatis.core.mapper.BaseMapperX;
import com.meession.etm.framework.mybatis.core.query.LambdaQueryWrapperX;
import com.meession.etm.module.crm.dal.dataobject.fulfillment.CrmErpCustomerMappingDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.Collection;
import java.util.List;

@Mapper
public interface CrmErpCustomerMappingMapper extends BaseMapperX<CrmErpCustomerMappingDO> {

    default CrmErpCustomerMappingDO selectByCrmCustomerId(Long crmCustomerId) {
        return selectOne(CrmErpCustomerMappingDO::getCrmCustomerId, crmCustomerId);
    }

    default CrmErpCustomerMappingDO selectByErpCustomerId(Long erpCustomerId) {
        return selectOne(CrmErpCustomerMappingDO::getErpCustomerId, erpCustomerId);
    }

    default List<CrmErpCustomerMappingDO> selectByCrmCustomerIds(Collection<Long> crmCustomerIds) {
        return selectList(new LambdaQueryWrapperX<CrmErpCustomerMappingDO>()
                .in(CrmErpCustomerMappingDO::getCrmCustomerId, crmCustomerIds));
    }

    default PageResult<CrmErpCustomerMappingDO> selectPage(PageParam pageParam) {
        return selectPage(pageParam, new LambdaQueryWrapperX<CrmErpCustomerMappingDO>()
                .orderByDesc(CrmErpCustomerMappingDO::getId));
    }
}
