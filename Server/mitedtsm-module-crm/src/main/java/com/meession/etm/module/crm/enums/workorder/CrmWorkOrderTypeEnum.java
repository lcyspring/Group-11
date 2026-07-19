package com.meession.etm.module.crm.enums.workorder;

import com.meession.etm.framework.common.core.ArrayValuable;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum CrmWorkOrderTypeEnum implements ArrayValuable<Integer> {
    ISSUE(1, "问题"), DEMAND(2, "需求"), COMPLAINT(3, "投诉"), CONSULTATION(4, "咨询");

    public static final Integer[] ARRAYS = java.util.Arrays.stream(values())
            .map(CrmWorkOrderTypeEnum::getType).toArray(Integer[]::new);
    private final Integer type;
    private final String name;
    @Override public Integer[] array() { return ARRAYS; }
}
