package com.meession.etm.module.crm.enums.customer;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 客户公海归属变更类型。
 */
@Getter
@AllArgsConstructor
public enum CrmCustomerOwnerRecordTypeEnum {

    PUT_POOL(1, "进入公海"),
    TAKE_POOL(2, "领取或分配");

    private final Integer type;
    private final String name;

}
