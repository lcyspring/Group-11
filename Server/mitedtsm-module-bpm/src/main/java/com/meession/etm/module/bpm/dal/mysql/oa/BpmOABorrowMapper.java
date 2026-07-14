package com.meession.etm.module.bpm.dal.mysql.oa;

import com.meession.etm.module.bpm.controller.admin.oa.vo.BpmOABorrowPageReqVO;
import com.meession.etm.module.bpm.dal.dataobject.oa.BpmOABorrowDO;
import com.meession.etm.framework.common.pojo.PageResult;
import com.meession.etm.framework.mybatis.core.mapper.BaseMapperX;
import com.meession.etm.framework.mybatis.core.query.LambdaQueryWrapperX;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface BpmOABorrowMapper extends BaseMapperX<BpmOABorrowDO> {

    default PageResult<BpmOABorrowDO> selectPage(Long userId, BpmOABorrowPageReqVO reqVO) {
        return selectPage(reqVO, new LambdaQueryWrapperX<BpmOABorrowDO>()
                .eqIfPresent(BpmOABorrowDO::getUserId, userId)
                .eqIfPresent(BpmOABorrowDO::getStatus, reqVO.getStatus())
                .likeIfPresent(BpmOABorrowDO::getReason, reqVO.getReason())
                .betweenIfPresent(BpmOABorrowDO::getCreateTime, reqVO.getCreateTime())
                .orderByDesc(BpmOABorrowDO::getId));
    }

}