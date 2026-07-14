package com.meession.etm.module.bpm.dal.mysql.oa;

import com.meession.etm.module.bpm.controller.admin.oa.vo.BpmOABusinessTripPageReqVO;
import com.meession.etm.module.bpm.dal.dataobject.oa.BpmOABusinessTripDO;
import com.meession.etm.framework.common.pojo.PageResult;
import com.meession.etm.framework.mybatis.core.mapper.BaseMapperX;
import com.meession.etm.framework.mybatis.core.query.LambdaQueryWrapperX;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface BpmOABusinessTripMapper extends BaseMapperX<BpmOABusinessTripDO> {

    default PageResult<BpmOABusinessTripDO> selectPage(Long userId, BpmOABusinessTripPageReqVO reqVO) {
        return selectPage(reqVO, new LambdaQueryWrapperX<BpmOABusinessTripDO>()
                .eqIfPresent(BpmOABusinessTripDO::getUserId, userId)
                .eqIfPresent(BpmOABusinessTripDO::getStatus, reqVO.getStatus())
                .likeIfPresent(BpmOABusinessTripDO::getDestination, reqVO.getDestination())
                .likeIfPresent(BpmOABusinessTripDO::getReason, reqVO.getReason())
                .betweenIfPresent(BpmOABusinessTripDO::getCreateTime, reqVO.getCreateTime())
                .orderByDesc(BpmOABusinessTripDO::getId));
    }

}