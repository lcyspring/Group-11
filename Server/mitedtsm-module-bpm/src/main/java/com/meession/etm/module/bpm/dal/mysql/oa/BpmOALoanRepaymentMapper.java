package com.meession.etm.module.bpm.dal.mysql.oa;

import com.meession.etm.framework.mybatis.core.mapper.BaseMapperX;
import com.meession.etm.module.bpm.dal.dataobject.oa.BpmOALoanRepaymentDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface BpmOALoanRepaymentMapper extends BaseMapperX<BpmOALoanRepaymentDO> {
    default List<BpmOALoanRepaymentDO> selectByLoanId(Long loanId) {
        return selectList(BpmOALoanRepaymentDO::getLoanId, loanId);
    }
}
