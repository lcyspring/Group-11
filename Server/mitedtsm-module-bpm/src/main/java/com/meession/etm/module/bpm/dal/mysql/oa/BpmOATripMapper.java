package com.meession.etm.module.bpm.dal.mysql.oa;

import com.meession.etm.framework.common.pojo.PageResult;
import com.meession.etm.framework.mybatis.core.mapper.BaseMapperX;
import com.meession.etm.framework.mybatis.core.query.LambdaQueryWrapperX;
import com.meession.etm.module.bpm.controller.admin.oa.vo.BpmOATripPageReqVO;
import com.meession.etm.module.bpm.dal.dataobject.oa.BpmOATripDO;
import org.apache.ibatis.annotations.Mapper;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface BpmOATripMapper extends BaseMapperX<BpmOATripDO> {
    default PageResult<BpmOATripDO> selectPage(Long userId, BpmOATripPageReqVO request) {
        return selectPage(request, new LambdaQueryWrapperX<BpmOATripDO>()
                .eq(BpmOATripDO::getUserId, userId)
                .eqIfPresent(BpmOATripDO::getStatus, request.getStatus())
                .likeIfPresent(BpmOATripDO::getDestination, request.getDestination())
                .betweenIfPresent(BpmOATripDO::getCreateTime, request.getCreateTime())
                .orderByDesc(BpmOATripDO::getId));
    }

    default List<BpmOATripDO> selectReimbursable(Long userId, Integer approvedStatus, LocalDateTime endedBefore) {
        return selectList(new LambdaQueryWrapperX<BpmOATripDO>()
                .eq(BpmOATripDO::getUserId, userId).eq(BpmOATripDO::getStatus, approvedStatus)
                .le(BpmOATripDO::getEndTime, endedBefore).orderByDesc(BpmOATripDO::getEndTime));
    }
}
