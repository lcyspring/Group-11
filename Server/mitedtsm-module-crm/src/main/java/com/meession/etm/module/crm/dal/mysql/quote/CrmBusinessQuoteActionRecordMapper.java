package com.meession.etm.module.crm.dal.mysql.quote;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.meession.etm.framework.mybatis.core.mapper.BaseMapperX;
import com.meession.etm.module.crm.dal.dataobject.quote.CrmBusinessQuoteActionRecordDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface CrmBusinessQuoteActionRecordMapper extends BaseMapperX<CrmBusinessQuoteActionRecordDO> {
    default List<CrmBusinessQuoteActionRecordDO> selectByQuoteId(Long quoteId) {
        return selectList(new LambdaQueryWrapper<CrmBusinessQuoteActionRecordDO>()
                .eq(CrmBusinessQuoteActionRecordDO::getQuoteId, quoteId)
                .orderByAsc(CrmBusinessQuoteActionRecordDO::getCreateTime)
                .orderByAsc(CrmBusinessQuoteActionRecordDO::getId));
    }
}
