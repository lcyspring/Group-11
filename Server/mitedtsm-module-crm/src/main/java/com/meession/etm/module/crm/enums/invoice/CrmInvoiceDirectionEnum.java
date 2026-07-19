package com.meession.etm.module.crm.enums.invoice;

import com.meession.etm.framework.common.core.ArrayValuable;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;

@Getter
@AllArgsConstructor
public enum CrmInvoiceDirectionEnum implements ArrayValuable<Integer> {

    BLUE(1, "蓝票"),
    RED(-1, "红票");

    private final Integer direction;
    private final String name;

    public static final Integer[] ARRAYS = Arrays.stream(values())
            .map(CrmInvoiceDirectionEnum::getDirection).toArray(Integer[]::new);

    @Override
    public Integer[] array() {
        return ARRAYS;
    }
}
