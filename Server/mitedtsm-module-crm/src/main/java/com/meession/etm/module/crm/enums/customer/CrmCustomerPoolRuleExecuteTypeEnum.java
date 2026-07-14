package com.meession.etm.module.crm.enums.customer;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjUtil;
import com.meession.etm.framework.common.core.ArrayValuable;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;

@Getter
@AllArgsConstructor
public enum CrmCustomerPoolRuleExecuteTypeEnum implements ArrayValuable<Integer> {

    SCHEDULED(1, "定时执行"),
    TRIGGER(2, "触发执行"),
    ;

    public static final Integer[] ARRAYS = Arrays.stream(values()).map(CrmCustomerPoolRuleExecuteTypeEnum::getType).toArray(Integer[]::new);

    private final Integer type;
    private final String name;

    public static String getNameByType(Integer type) {
        CrmCustomerPoolRuleExecuteTypeEnum typeEnum = CollUtil.findOne(CollUtil.newArrayList(CrmCustomerPoolRuleExecuteTypeEnum.values()),
                item -> ObjUtil.equal(item.type, type));
        return typeEnum == null ? null : typeEnum.getName();
    }

    @Override
    public Integer[] array() {
        return ARRAYS;
    }

}
