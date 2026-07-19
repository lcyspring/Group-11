package com.meession.etm.module.crm.enums.customer;

import com.meession.etm.framework.common.core.ArrayValuable;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;

/**
 * CRM 客户生命周期状态。
 */
@Getter
@AllArgsConstructor
public enum CrmCustomerLifecycleStatusEnum implements ArrayValuable<Integer> {

    POTENTIAL(10, "潜在客户"),
    INTENTIONAL(20, "意向客户"),
    DEAL(30, "成交客户"),
    LOST(40, "流失客户");

    public static final Integer[] ARRAYS = Arrays.stream(values())
            .map(CrmCustomerLifecycleStatusEnum::getStatus).toArray(Integer[]::new);

    private final Integer status;
    private final String name;

    public static boolean isDeal(Integer status) {
        return DEAL.status.equals(status);
    }

    public static boolean isLost(Integer status) {
        return LOST.status.equals(status);
    }

    public static boolean isValid(Integer status) {
        return Arrays.asList(ARRAYS).contains(status);
    }

    public static String getNameByStatus(Integer status) {
        return Arrays.stream(values()).filter(item -> item.status.equals(status))
                .map(CrmCustomerLifecycleStatusEnum::getName).findFirst().orElse("未知状态");
    }

    @Override
    public Integer[] array() {
        return ARRAYS;
    }

}
