package com.meession.etm.module.crm.enums.customer;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjUtil;
import com.meession.etm.framework.common.core.ArrayValuable;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;

@Getter
@AllArgsConstructor
public enum CrmCustomerPoolRuleTypeEnum implements ArrayValuable<Integer> {

    RECYCLE(1, "回收规则"),
    RECEIVE(2, "领取规则"),
    DISTRIBUTE(3, "分配规则"),
    ;

    public static final Integer[] ARRAYS = Arrays.stream(values()).map(CrmCustomerPoolRuleTypeEnum::getType).toArray(Integer[]::new);

    private final Integer type;
    private final String name;

    public static String getNameByType(Integer type) {
        CrmCustomerPoolRuleTypeEnum typeEnum = CollUtil.findOne(CollUtil.newArrayList(CrmCustomerPoolRuleTypeEnum.values()),
                item -> ObjUtil.equal(item.type, type));
        return typeEnum == null ? null : typeEnum.getName();
    }

    @Override
    public Integer[] array() {
        return ARRAYS;
    }

}
