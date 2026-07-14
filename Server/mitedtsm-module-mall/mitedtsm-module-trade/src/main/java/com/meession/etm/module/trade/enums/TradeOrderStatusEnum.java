package com.meession.etm.module.trade.enums;

import com.meession.etm.framework.common.core.ArrayValuable;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;

@Getter
@AllArgsConstructor
public enum TradeOrderStatusEnum implements ArrayValuable<Integer> {

    CREATE(0, "待付款"),
    PAID(1, "待发货"),
    DELIVERY(2, "待收货"),
    RECEIVE(3, "已完成"),
    CANCEL(4, "已取消"),
    REFUND(5, "退款中"),
    REFUNDED(6, "已退款");

    public static final Integer[] ARRAYS = Arrays.stream(values()).map(TradeOrderStatusEnum::getStatus).toArray(Integer[]::new);

    private final Integer status;
    private final String name;

    @Override
    public Integer[] array() {
        return ARRAYS;
    }

    public static TradeOrderStatusEnum valueOf(Integer status) {
        for (TradeOrderStatusEnum enumValue : values()) {
            if (enumValue.status.equals(status)) {
                return enumValue;
            }
        }
        return null;
    }

}