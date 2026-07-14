package com.meession.etm.module.crm.enums.customer;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjUtil;
import com.meession.etm.framework.common.core.ArrayValuable;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;

@Getter
@AllArgsConstructor
public enum CrmCustomerPoolStatusEnum implements ArrayValuable<Integer> {

    NOT_IN_POOL(0, "非公海"),
    IN_POOL(1, "公海"),
    ;

    public static final Integer[] ARRAYS = Arrays.stream(values()).map(CrmCustomerPoolStatusEnum::getStatus).toArray(Integer[]::new);

    private final Integer status;
    private final String name;

    public static String getNameByStatus(Integer status) {
        CrmCustomerPoolStatusEnum statusEnum = CollUtil.findOne(CollUtil.newArrayList(CrmCustomerPoolStatusEnum.values()),
                item -> ObjUtil.equal(item.status, status));
        return statusEnum == null ? null : statusEnum.getName();
    }

    @Override
    public Integer[] array() {
        return ARRAYS;
    }

}
