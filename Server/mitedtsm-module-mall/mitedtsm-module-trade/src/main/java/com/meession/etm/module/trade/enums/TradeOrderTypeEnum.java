package com.meession.etm.module.trade.enums;

import com.meession.etm.framework.common.core.ArrayValuable;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;

@Getter
@AllArgsConstructor
public enum TradeOrderTypeEnum implements ArrayValuable<Integer> {

    NORMAL(0, "普通订单"),
    SECKILL(1, "秒杀订单"),
    BARGAIN(2, "砍价订单"),
    COMBINATION(3, "拼团订单");

    public static final Integer[] ARRAYS = Arrays.stream(values()).map(TradeOrderTypeEnum::getType).toArray(Integer[]::new);

    private final Integer type;
    private final String name;

    @Override
    public Integer[] array() {
        return ARRAYS;
    }

    public static TradeOrderTypeEnum valueOf(Integer type) {
        for (TradeOrderTypeEnum enumValue : values()) {
            if (enumValue.type.equals(type)) {
                return enumValue;
            }
        }
        return null;
    }

}