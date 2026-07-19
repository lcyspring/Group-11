package com.meession.etm.module.crm.enums.workorder;

import com.meession.etm.framework.common.core.ArrayValuable;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum CrmWorkOrderStatusEnum implements ArrayValuable<Integer> {
    PENDING(10, "待处理"),
    PROCESSING(20, "处理中"),
    COMPLETED(30, "已完结"),
    RETURNED(40, "已退回");

    public static final Integer[] ARRAYS = java.util.Arrays.stream(values())
            .map(CrmWorkOrderStatusEnum::getStatus).toArray(Integer[]::new);

    private final Integer status;
    private final String name;

    @Override
    public Integer[] array() {
        return ARRAYS;
    }
}
