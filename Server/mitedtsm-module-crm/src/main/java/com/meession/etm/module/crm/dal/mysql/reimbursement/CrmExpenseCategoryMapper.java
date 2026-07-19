package com.meession.etm.module.crm.dal.mysql.reimbursement;

import com.meession.etm.framework.mybatis.core.mapper.BaseMapperX;
import com.meession.etm.framework.mybatis.core.query.LambdaQueryWrapperX;
import com.meession.etm.module.crm.dal.dataobject.reimbursement.CrmExpenseCategoryDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface CrmExpenseCategoryMapper extends BaseMapperX<CrmExpenseCategoryDO> {
    default CrmExpenseCategoryDO selectByCode(String code) {
        return selectOne(CrmExpenseCategoryDO::getCode, code);
    }

    default CrmExpenseCategoryDO selectByName(String name) {
        return selectOne(CrmExpenseCategoryDO::getName, name);
    }

    default List<CrmExpenseCategoryDO> selectListByStatus(Integer status) {
        return selectList(new LambdaQueryWrapperX<CrmExpenseCategoryDO>()
                .eqIfPresent(CrmExpenseCategoryDO::getStatus, status)
                .orderByAsc(CrmExpenseCategoryDO::getSort)
                .orderByAsc(CrmExpenseCategoryDO::getId));
    }
}
