package com.meession.etm.module.crm.enums.invoice;

import com.meession.etm.framework.common.core.ArrayValuable;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;

@Getter
@AllArgsConstructor
public enum CrmInvoiceStatusEnum implements ArrayValuable<Integer> {

    DRAFT(0, "草稿"),
    ISSUED(10, "已开具"),
    PARTIALLY_RED(20, "部分红冲"),
    FULLY_RED(30, "已全部红冲"),
    VOIDED(40, "已作废");

    private final Integer status;
    private final String name;

    public static final Integer[] ARRAYS = Arrays.stream(values())
            .map(CrmInvoiceStatusEnum::getStatus).toArray(Integer[]::new);

    @Override
    public Integer[] array() {
        return ARRAYS;
    }
}
