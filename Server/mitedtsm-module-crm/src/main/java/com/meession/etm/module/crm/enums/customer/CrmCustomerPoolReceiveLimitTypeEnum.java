package com.meession.etm.module.crm.enums.customer;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjUtil;
import com.meession.etm.framework.common.core.ArrayValuable;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;

@Getter
@AllArgsConstructor
public enum CrmCustomerPoolReceiveLimitTypeEnum implements ArrayValuable<Integer> {

    DAILY(1, "每日限制"),
    WEEKLY(2, "每周限制"),
    MONTHLY(3, "每月限制"),
    ;

    public static final Integer[] ARRAYS = Arrays.stream(values()).map(CrmCustomerPoolReceiveLimitTypeEnum::getType).toArray(Integer[]::new);

    private final Integer type;
    private final String name;

    public static String getNameByType(Integer type) {
        CrmCustomerPoolReceiveLimitTypeEnum typeEnum = CollUtil.findOne(CollUtil.newArrayList(CrmCustomerPoolReceiveLimitTypeEnum.values()),
                item -> ObjUtil.equal(item.type, type));
        return typeEnum == null ? null : typeEnum.getName();
    }

    @Override
    public Integer[] array() {
        return ARRAYS;
    }

}
