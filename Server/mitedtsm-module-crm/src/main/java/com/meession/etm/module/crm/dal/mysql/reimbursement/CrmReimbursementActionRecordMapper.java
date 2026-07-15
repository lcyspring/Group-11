package com.meession.etm.module.crm.dal.mysql.reimbursement;

import com.meession.etm.framework.mybatis.core.mapper.BaseMapperX;
import com.meession.etm.framework.mybatis.core.query.LambdaQueryWrapperX;
import com.meession.etm.module.crm.dal.dataobject.reimbursement.CrmReimbursementActionRecordDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface CrmReimbursementActionRecordMapper extends BaseMapperX<CrmReimbursementActionRecordDO> {
    default List<CrmReimbursementActionRecordDO> selectListByReimbursementId(Long reimbursementId) {
        return selectList(new LambdaQueryWrapperX<CrmReimbursementActionRecordDO>()
                .eq(CrmReimbursementActionRecordDO::getReimbursementId, reimbursementId)
                .orderByAsc(CrmReimbursementActionRecordDO::getId));
    }

    default long selectCountByReimbursementIdAndAction(Long reimbursementId, Integer actionType) {
        return selectCount(new LambdaQueryWrapperX<CrmReimbursementActionRecordDO>()
                .eq(CrmReimbursementActionRecordDO::getReimbursementId, reimbursementId)
                .eq(CrmReimbursementActionRecordDO::getActionType, actionType));
    }
}
