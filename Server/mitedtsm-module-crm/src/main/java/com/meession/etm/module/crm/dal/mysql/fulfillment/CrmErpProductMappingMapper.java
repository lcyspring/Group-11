package com.meession.etm.module.crm.dal.mysql.fulfillment;

import com.meession.etm.framework.common.pojo.PageParam;
import com.meession.etm.framework.common.pojo.PageResult;
import com.meession.etm.framework.mybatis.core.mapper.BaseMapperX;
import com.meession.etm.framework.mybatis.core.query.LambdaQueryWrapperX;
import com.meession.etm.module.crm.dal.dataobject.fulfillment.CrmErpProductMappingDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.Collection;
import java.util.List;

@Mapper
public interface CrmErpProductMappingMapper extends BaseMapperX<CrmErpProductMappingDO> {

    default CrmErpProductMappingDO selectByCrmProductId(Long crmProductId) {
        return selectOne(CrmErpProductMappingDO::getCrmProductId, crmProductId);
    }

    default CrmErpProductMappingDO selectByErpProductId(Long erpProductId) {
        return selectOne(CrmErpProductMappingDO::getErpProductId, erpProductId);
    }

    default List<CrmErpProductMappingDO> selectByCrmProductIds(Collection<Long> crmProductIds) {
        return selectList(new LambdaQueryWrapperX<CrmErpProductMappingDO>()
                .in(CrmErpProductMappingDO::getCrmProductId, crmProductIds));
    }

    default PageResult<CrmErpProductMappingDO> selectPage(PageParam pageParam) {
        return selectPage(pageParam, new LambdaQueryWrapperX<CrmErpProductMappingDO>()
                .orderByDesc(CrmErpProductMappingDO::getId));
    }
}
