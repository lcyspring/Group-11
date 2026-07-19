package com.meession.etm.module.bpm.dal.mysql.oa;

import com.meession.etm.framework.mybatis.core.mapper.BaseMapperX;
import com.meession.etm.framework.mybatis.core.query.LambdaQueryWrapperX;
import com.meession.etm.module.bpm.dal.dataobject.oa.BpmOALeaveCalendarDO;
import org.apache.ibatis.annotations.Mapper;

import java.time.LocalDate;
import java.util.List;

@Mapper
public interface BpmOALeaveCalendarMapper extends BaseMapperX<BpmOALeaveCalendarDO> {
    default List<BpmOALeaveCalendarDO> selectRange(LocalDate start, LocalDate end) {
        return selectList(new LambdaQueryWrapperX<BpmOALeaveCalendarDO>()
                .between(BpmOALeaveCalendarDO::getCalendarDate, start, end));
    }
}
