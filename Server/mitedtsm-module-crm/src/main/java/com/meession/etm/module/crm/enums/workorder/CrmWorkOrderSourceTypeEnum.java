package com.meession.etm.module.crm.enums.workorder;

import com.meession.etm.framework.common.core.ArrayValuable;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum CrmWorkOrderSourceTypeEnum implements ArrayValuable<Integer> {
    CUSTOMER(0, "客户"), BUSINESS(1, "商机"), CONTRACT(2, "合同");

    public static final Integer[] ARRAYS = java.util.Arrays.stream(values())
            .map(CrmWorkOrderSourceTypeEnum::getType).toArray(Integer[]::new);
    private final Integer type;
    private final String name;
    @Override public Integer[] array() { return ARRAYS; }
}
