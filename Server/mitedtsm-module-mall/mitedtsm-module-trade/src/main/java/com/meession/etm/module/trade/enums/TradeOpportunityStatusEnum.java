package com.meession.etm.module.trade.enums;

import com.meession.etm.framework.common.core.ArrayValuable;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;

@Getter
@AllArgsConstructor
public enum TradeOpportunityStatusEnum implements ArrayValuable<Integer> {

    PENDING(0, "待跟进"),
    CONTACTING(1, "沟通中"),
    QUOTED(2, "已报价"),
    NEGOTIATING(3, "谈判中"),
    WON(4, "已成交"),
    LOST(5, "已流失");

    public static final Integer[] ARRAYS = Arrays.stream(values()).map(TradeOpportunityStatusEnum::getStatus).toArray(Integer[]::new);

    private final Integer status;
    private final String name;

    @Override
    public Integer[] array() {
        return ARRAYS;
    }

    public static TradeOpportunityStatusEnum valueOf(Integer status) {
        for (TradeOpportunityStatusEnum enumValue : values()) {
            if (enumValue.status.equals(status)) {
                return enumValue;
            }
        }
        return null;
    }

}