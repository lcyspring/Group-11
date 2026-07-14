package com.meession.etm.module.crm.enums.statistics;

import com.meession.etm.framework.common.core.ArrayValuable;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Arrays;

/**
 * CRM 业绩目标指标类型。
 */
@RequiredArgsConstructor
@Getter
public enum CrmPerformanceTargetTypeEnum implements ArrayValuable<Integer> {

    CONTRACT_PRICE(1, "成交金额", false),
    RECEIVABLE_PRICE(2, "回款金额", false),
    FOLLOW_UP_COUNT(3, "跟进次数", true),
    CUSTOMER_COUNT(4, "新增客户", true),
    BUSINESS_COUNT(5, "新增商机", true);

    public static final Integer[] ARRAYS = Arrays.stream(values())
            .map(CrmPerformanceTargetTypeEnum::getType).toArray(Integer[]::new);

    private final Integer type;
    private final String name;
    private final boolean count;

    public static CrmPerformanceTargetTypeEnum fromType(Integer type) {
        return Arrays.stream(values()).filter(value -> value.type.equals(type)).findFirst().orElse(null);
    }

    @Override
    public Integer[] array() {
        return ARRAYS;
    }

}
