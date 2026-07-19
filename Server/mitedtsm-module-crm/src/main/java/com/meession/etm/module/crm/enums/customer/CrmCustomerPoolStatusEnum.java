package com.meession.etm.module.crm.enums.customer;

import com.meession.etm.framework.common.core.ArrayValuable;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;

/** Current ownership-pool state of a customer. */
@Getter
@AllArgsConstructor
public enum CrmCustomerPoolStatusEnum implements ArrayValuable<Integer> {

    OWNED(0, "在管"),
    PUBLIC(1, "公海"),
    GARBAGE(2, "垃圾池");

    public static final Integer[] ARRAYS = Arrays.stream(values())
            .map(CrmCustomerPoolStatusEnum::getStatus).toArray(Integer[]::new);

    private final Integer status;
    private final String name;

    @Override
    public Integer[] array() {
        return ARRAYS;
    }
}
