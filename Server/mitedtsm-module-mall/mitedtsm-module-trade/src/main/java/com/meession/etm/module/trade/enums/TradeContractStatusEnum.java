package com.meession.etm.module.trade.enums;

import com.meession.etm.framework.common.core.ArrayValuable;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;

@Getter
@AllArgsConstructor
public enum TradeContractStatusEnum implements ArrayValuable<Integer> {

    DRAFT(0, "草稿"),
    PENDING_SIGN(1, "待签署"),
    SIGNED(2, "已签署"),
    EXECUTING(3, "执行中"),
    COMPLETED(4, "已完成"),
    TERMINATED(5, "已终止"),
    EXPIRED(6, "已过期");

    public static final Integer[] ARRAYS = Arrays.stream(values()).map(TradeContractStatusEnum::getStatus).toArray(Integer[]::new);

    private final Integer status;
    private final String name;

    @Override
    public Integer[] array() {
        return ARRAYS;
    }

    public static TradeContractStatusEnum valueOf(Integer status) {
        for (TradeContractStatusEnum enumValue : values()) {
            if (enumValue.status.equals(status)) {
                return enumValue;
            }
        }
        return null;
    }

}