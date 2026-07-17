package com.meession.etm.module.bpm.dal.mysql.oa;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.meession.etm.framework.common.pojo.PageResult;
import com.meession.etm.framework.mybatis.core.mapper.BaseMapperX;
import com.meession.etm.framework.mybatis.core.query.LambdaQueryWrapperX;
import com.meession.etm.module.bpm.controller.admin.oa.vo.BpmOALoanPageReqVO;
import com.meession.etm.module.bpm.dal.dataobject.oa.BpmOALoanDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface BpmOALoanMapper extends BaseMapperX<BpmOALoanDO> {
    default PageResult<BpmOALoanDO> selectPage(Long userId, BpmOALoanPageReqVO request) {
        return selectPage(request, new LambdaQueryWrapperX<BpmOALoanDO>()
                .eq(BpmOALoanDO::getUserId, userId)
                .eqIfPresent(BpmOALoanDO::getType, request.getType())
                .eqIfPresent(BpmOALoanDO::getStatus, request.getStatus())
                .eqIfPresent(BpmOALoanDO::getRepaymentStatus, request.getRepaymentStatus())
                .orderByDesc(BpmOALoanDO::getId));
    }

    default List<BpmOALoanDO> selectOutstandingForUpdate(Long userId) {
        return selectList(new LambdaQueryWrapper<BpmOALoanDO>().eq(BpmOALoanDO::getUserId, userId)
                .gt(BpmOALoanDO::getOutstandingAmount, 0).eq(BpmOALoanDO::getRepaymentStatus, 1)
                .orderByAsc(BpmOALoanDO::getId).last("FOR UPDATE"));
    }

    default BpmOALoanDO selectByIdForUpdate(Long id) {
        return selectOne(new LambdaQueryWrapper<BpmOALoanDO>().eq(BpmOALoanDO::getId, id).last("FOR UPDATE"));
    }
}
