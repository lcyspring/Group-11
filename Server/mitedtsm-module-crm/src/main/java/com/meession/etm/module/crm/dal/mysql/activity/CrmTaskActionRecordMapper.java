package com.meession.etm.module.crm.dal.mysql.activity;

import com.meession.etm.framework.mybatis.core.mapper.BaseMapperX;
import com.meession.etm.framework.mybatis.core.query.LambdaQueryWrapperX;
import com.meession.etm.module.crm.dal.dataobject.activity.CrmTaskActionRecordDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.Collection;
import java.util.List;

@Mapper
public interface CrmTaskActionRecordMapper extends BaseMapperX<CrmTaskActionRecordDO> {
    default List<CrmTaskActionRecordDO> selectListByTaskIds(Collection<Long> taskIds) {
        return selectList(new LambdaQueryWrapperX<CrmTaskActionRecordDO>()
                .inIfPresent(CrmTaskActionRecordDO::getTaskId, taskIds)
                .orderByAsc(CrmTaskActionRecordDO::getCreateTime)
                .orderByAsc(CrmTaskActionRecordDO::getId));
    }
}
