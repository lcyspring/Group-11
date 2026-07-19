package com.meession.etm.module.trade.convert.order;

import com.meession.etm.module.trade.dal.dataobject.order.TradeOrderLogDO;
import com.meession.etm.module.trade.service.order.bo.TradeOrderLogCreateReqBO;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

@Mapper(config = com.meession.etm.framework.common.mapstruct.MitedtsmMapperConfig.class)
public interface TradeOrderLogConvert {

    TradeOrderLogConvert INSTANCE = Mappers.getMapper(TradeOrderLogConvert.class);

    TradeOrderLogDO convert(TradeOrderLogCreateReqBO bean);

}
