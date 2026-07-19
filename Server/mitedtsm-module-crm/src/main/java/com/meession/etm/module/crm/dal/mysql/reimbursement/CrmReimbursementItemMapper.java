package com.meession.etm.module.crm.dal.mysql.reimbursement;

import com.meession.etm.framework.mybatis.core.mapper.BaseMapperX;
import com.meession.etm.framework.mybatis.core.query.LambdaQueryWrapperX;
import com.meession.etm.module.crm.dal.dataobject.reimbursement.CrmReimbursementItemDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface CrmReimbursementItemMapper extends BaseMapperX<CrmReimbursementItemDO> {
    default List<CrmReimbursementItemDO> selectListByReimbursementId(Long reimbursementId) {
        return selectList(new LambdaQueryWrapperX<CrmReimbursementItemDO>()
                .eq(CrmReimbursementItemDO::getReimbursementId, reimbursementId)
                .orderByAsc(CrmReimbursementItemDO::getSort)
                .orderByAsc(CrmReimbursementItemDO::getId));
    }

    default void deleteByReimbursementId(Long reimbursementId) {
        delete(CrmReimbursementItemDO::getReimbursementId, reimbursementId);
    }

    default long selectCountByCategoryId(Long categoryId) {
        return selectCount(CrmReimbursementItemDO::getCategoryId, categoryId);
    }
}
