package com.meession.etm.module.crm.enums.customer;

import com.meession.etm.framework.common.core.ArrayValuable;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;

/**
 * CRM 客户等级
 *
 * @author Wanwan
 */
@Getter
@AllArgsConstructor
public enum CrmCustomerLevelEnum implements ArrayValuable<Integer> {

    NORMAL(1, "普通客户"),
    BRONZE(2, "铜牌客户"),
    SILVER(3, "银牌客户"),
    GOLD(4, "金牌客户"),
    VIP(5, "VIP 客户");

    public static final Integer[] ARRAYS = Arrays.stream(values()).map(CrmCustomerLevelEnum::getLevel).toArray(Integer[]::new);

    /**
     * 状态
     */
    private final Integer level;
    /**
     * 状态名
     */
    private final String name;

    @Override
    public Integer[] array() {
        return ARRAYS;
    }

}
