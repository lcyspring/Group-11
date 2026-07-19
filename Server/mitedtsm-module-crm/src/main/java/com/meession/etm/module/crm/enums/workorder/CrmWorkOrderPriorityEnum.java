package com.meession.etm.module.crm.enums.workorder;

import com.meession.etm.framework.common.core.ArrayValuable;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum CrmWorkOrderPriorityEnum implements ArrayValuable<Integer> {
    LOW(1, "低"), MEDIUM(2, "中"), HIGH(3, "高");

    public static final Integer[] ARRAYS = java.util.Arrays.stream(values())
            .map(CrmWorkOrderPriorityEnum::getPriority).toArray(Integer[]::new);
    private final Integer priority;
    private final String name;
    @Override public Integer[] array() { return ARRAYS; }
}
