package com.meession.etm.module.crm.dal.mysql.customer;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.meession.etm.framework.mybatis.core.mapper.BaseMapperX;
import com.meession.etm.module.crm.dal.dataobject.customer.CrmCustomerImportPreviewDO;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface CrmCustomerImportPreviewMapper extends BaseMapperX<CrmCustomerImportPreviewDO> {

    default CrmCustomerImportPreviewDO selectByIdForUpdate(Long id) {
        return selectOne(new LambdaQueryWrapper<CrmCustomerImportPreviewDO>()
                .eq(CrmCustomerImportPreviewDO::getId, id)
                .last("FOR UPDATE"));
    }
}
