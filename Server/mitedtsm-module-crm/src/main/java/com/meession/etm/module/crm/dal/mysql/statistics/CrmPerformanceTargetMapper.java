package com.meession.etm.module.crm.dal.mysql.statistics;

import com.meession.etm.framework.mybatis.core.mapper.BaseMapperX;
import com.meession.etm.framework.mybatis.core.query.LambdaQueryWrapperX;
import com.meession.etm.module.crm.dal.dataobject.statistics.CrmPerformanceTargetDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface CrmPerformanceTargetMapper extends BaseMapperX<CrmPerformanceTargetDO> {

    default List<CrmPerformanceTargetDO> selectListByScopeAndYear(Integer scopeType, Long scopeId,
                                                                   Integer targetYear, Integer targetType) {
        return selectList(new LambdaQueryWrapperX<CrmPerformanceTargetDO>()
                .eq(CrmPerformanceTargetDO::getScopeType, scopeType)
                .eq(CrmPerformanceTargetDO::getScopeId, scopeId)
                .eq(CrmPerformanceTargetDO::getTargetYear, targetYear)
                .eqIfPresent(CrmPerformanceTargetDO::getTargetType, targetType)
                .orderByAsc(CrmPerformanceTargetDO::getTargetType)
                .orderByAsc(CrmPerformanceTargetDO::getTargetMonth));
    }

    default void deleteByScopeYearAndType(Integer scopeType, Long scopeId, Integer targetYear,
                                          Integer targetType) {
        delete(new LambdaQueryWrapperX<CrmPerformanceTargetDO>()
                .eq(CrmPerformanceTargetDO::getScopeType, scopeType)
                .eq(CrmPerformanceTargetDO::getScopeId, scopeId)
                .eq(CrmPerformanceTargetDO::getTargetYear, targetYear)
                .eq(CrmPerformanceTargetDO::getTargetType, targetType));
    }

}
