package com.meession.etm.module.bpm.dal.mysql.oa;

import com.meession.etm.framework.mybatis.core.mapper.BaseMapperX;
import com.meession.etm.framework.mybatis.core.query.LambdaQueryWrapperX;
import com.meession.etm.module.bpm.dal.dataobject.oa.BpmOAEventDO;
import org.apache.ibatis.annotations.Mapper;
import java.util.List;

@Mapper
public interface BpmOAEventMapper extends BaseMapperX<BpmOAEventDO> {
    default List<BpmOAEventDO> selectByUserId(Long userId, java.time.LocalDateTime from, java.time.LocalDateTime to) {
        return selectList(new LambdaQueryWrapperX<BpmOAEventDO>().eq(BpmOAEventDO::getUserId, userId)
                .ne(BpmOAEventDO::getStatus, 10).lt(BpmOAEventDO::getStartTime, to)
                .gt(BpmOAEventDO::getEndTime, from).orderByAsc(BpmOAEventDO::getStartTime));
    }
}
