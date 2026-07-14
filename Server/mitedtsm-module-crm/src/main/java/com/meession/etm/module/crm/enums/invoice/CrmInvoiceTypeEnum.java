package com.meession.etm.module.crm.enums.invoice;

import com.meession.etm.framework.common.core.ArrayValuable;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;

@Getter
@AllArgsConstructor
public enum CrmInvoiceTypeEnum implements ArrayValuable<Integer> {

    VAT_ORDINARY(1, "增值税普通发票"),
    VAT_SPECIAL(2, "增值税专用发票");

    private final Integer type;
    private final String name;

    public static final Integer[] ARRAYS = Arrays.stream(values())
            .map(CrmInvoiceTypeEnum::getType).toArray(Integer[]::new);

    @Override
    public Integer[] array() {
        return ARRAYS;
    }
}
