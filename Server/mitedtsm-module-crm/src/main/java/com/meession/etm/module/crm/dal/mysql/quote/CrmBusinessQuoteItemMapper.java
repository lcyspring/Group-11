package com.meession.etm.module.crm.dal.mysql.quote;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.meession.etm.framework.mybatis.core.mapper.BaseMapperX;
import com.meession.etm.module.crm.dal.dataobject.quote.CrmBusinessQuoteItemDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface CrmBusinessQuoteItemMapper extends BaseMapperX<CrmBusinessQuoteItemDO> {
    default List<CrmBusinessQuoteItemDO> selectByQuoteId(Long quoteId) {
        return selectList(new LambdaQueryWrapper<CrmBusinessQuoteItemDO>()
                .eq(CrmBusinessQuoteItemDO::getQuoteId, quoteId).orderByAsc(CrmBusinessQuoteItemDO::getId));
    }

    default void deleteByQuoteId(Long quoteId) {
        delete(new LambdaQueryWrapper<CrmBusinessQuoteItemDO>().eq(CrmBusinessQuoteItemDO::getQuoteId, quoteId));
    }
}
