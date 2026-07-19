package com.meession.etm.module.crm.enums.receivable;

import com.meession.etm.framework.common.core.ArrayValuable;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;

@Getter
@AllArgsConstructor
public enum CrmReceivableWriteOffSourceTypeEnum implements ArrayValuable<Integer> {

    MANUAL(1, "人工登记"),
    BANK_TRANSACTION(2, "银行流水"),
    BATCH_IMPORT(3, "批量导入");

    private static final Integer[] ARRAYS = Arrays.stream(values())
            .map(CrmReceivableWriteOffSourceTypeEnum::getType).toArray(Integer[]::new);

    private final Integer type;
    private final String name;

    @Override
    public Integer[] array() {
        return ARRAYS;
    }
}
