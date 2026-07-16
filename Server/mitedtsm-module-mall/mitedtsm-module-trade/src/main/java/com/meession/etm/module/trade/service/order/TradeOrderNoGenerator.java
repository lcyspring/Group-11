package com.meession.etm.module.trade.service.order;

import com.meession.etm.module.trade.dal.redis.no.TradeNoRedisDAO;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Component;

@Component
public class TradeOrderNoGenerator {

    @Resource
    private TradeNoRedisDAO tradeNoRedisDAO;

    public String generateOrderNo() {
        return tradeNoRedisDAO.generate(TradeNoRedisDAO.TRADE_ORDER_NO_PREFIX);
    }

    public String generateAfterSaleNo() {
        return tradeNoRedisDAO.generate(TradeNoRedisDAO.AFTER_SALE_NO_PREFIX);
    }

}