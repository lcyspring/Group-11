package com.meession.etm.module.crm.enums.customer;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjUtil;
import com.meession.etm.framework.common.core.ArrayValuable;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;

@Getter
@AllArgsConstructor
public enum CrmCustomerPoolReceiveSourceTypeEnum implements ArrayValuable<Integer> {

    MANUAL(1, "手动领取"),
    AUTO(2, "自动分配"),
    ADMIN(3, "管理员分配"),
    ;

    public static final Integer[] ARRAYS = Arrays.stream(values()).map(CrmCustomerPoolReceiveSourceTypeEnum::getType).toArray(Integer[]::new);

    private final Integer type;
    private final String name;

    public static String getNameByType(Integer type) {
        CrmCustomerPoolReceiveSourceTypeEnum typeEnum = CollUtil.findOne(CollUtil.newArrayList(CrmCustomerPoolReceiveSourceTypeEnum.values()),
                item -> ObjUtil.equal(item.type, type));
        return typeEnum == null ? null : typeEnum.getName();
    }

    @Override
    public Integer[] array() {
        return ARRAYS;
    }

}
