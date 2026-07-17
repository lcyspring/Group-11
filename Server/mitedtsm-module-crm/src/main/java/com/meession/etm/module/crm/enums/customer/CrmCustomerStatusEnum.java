package com.meession.etm.module.crm.enums.customer;

import com.meession.etm.framework.common.core.ArrayValuable;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;

@Getter
@AllArgsConstructor
public enum CrmCustomerStatusEnum implements ArrayValuable<Integer> {

    POTENTIAL(1, "潜在客户"),
    INTENTION(2, "意向客户"),
    NEGOTIATION(3, "谈判中"),
    DEAL(4, "已成交"),
    LOST(5, "流失客户");

    public static final Integer[] ARRAYS = Arrays.stream(values()).map(CrmCustomerStatusEnum::getStatus).toArray(Integer[]::new);

    private final Integer status;
    private final String name;

    @Override
    public Integer[] array() {
        return ARRAYS;
    }

}