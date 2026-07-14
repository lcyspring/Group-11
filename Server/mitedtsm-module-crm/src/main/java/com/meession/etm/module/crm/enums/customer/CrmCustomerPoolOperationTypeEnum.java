package com.meession.etm.module.crm.enums.customer;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjUtil;
import com.meession.etm.framework.common.core.ArrayValuable;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;

@Getter
@AllArgsConstructor
public enum CrmCustomerPoolOperationTypeEnum implements ArrayValuable<Integer> {

    PUT(1, "放入公海"),
    RECEIVE(2, "领取公海"),
    AUTO_RECYCLE(3, "自动回收"),
    DISTRIBUTE(4, "分配公海"),
    ;

    public static final Integer[] ARRAYS = Arrays.stream(values()).map(CrmCustomerPoolOperationTypeEnum::getType).toArray(Integer[]::new);

    private final Integer type;
    private final String name;

    public static String getNameByType(Integer type) {
        CrmCustomerPoolOperationTypeEnum typeEnum = CollUtil.findOne(CollUtil.newArrayList(CrmCustomerPoolOperationTypeEnum.values()),
                item -> ObjUtil.equal(item.type, type));
        return typeEnum == null ? null : typeEnum.getName();
    }

    @Override
    public Integer[] array() {
        return ARRAYS;
    }

}
