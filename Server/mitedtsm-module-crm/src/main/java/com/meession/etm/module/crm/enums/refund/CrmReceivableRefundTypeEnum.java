package com.meession.etm.module.crm.enums.refund;

import com.meession.etm.framework.common.core.ArrayValuable;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Arrays;

@Getter
@RequiredArgsConstructor
public enum CrmReceivableRefundTypeEnum implements ArrayValuable<Integer> {

    CUSTOMER_REFUND(1, "客户退款"),
    BUSINESS_REVERSAL(2, "业务冲销");

    private final Integer type;
    private final String name;

    private static final Integer[] ARRAYS = Arrays.stream(values())
            .map(CrmReceivableRefundTypeEnum::getType).toArray(Integer[]::new);

    @Override
    public Integer[] array() {
        return ARRAYS;
    }
}
