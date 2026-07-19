package com.meession.etm.module.crm.enums.statistics;

import com.meession.etm.framework.common.core.ArrayValuable;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Arrays;

/**
 * CRM 业绩目标范围类型。
 */
@RequiredArgsConstructor
@Getter
public enum CrmPerformanceTargetScopeTypeEnum implements ArrayValuable<Integer> {

    COMPANY(1, "公司"),
    DEPARTMENT(2, "部门"),
    USER(3, "个人");

    public static final Integer[] ARRAYS = Arrays.stream(values())
            .map(CrmPerformanceTargetScopeTypeEnum::getType).toArray(Integer[]::new);

    private final Integer type;
    private final String name;

    public static CrmPerformanceTargetScopeTypeEnum fromType(Integer type) {
        return Arrays.stream(values()).filter(value -> value.type.equals(type)).findFirst().orElse(null);
    }

    @Override
    public Integer[] array() {
        return ARRAYS;
    }

}
