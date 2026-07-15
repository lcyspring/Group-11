package com.meession.etm.module.crm.enums.customer;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 客户归属变更类型。
 */
@Getter
@AllArgsConstructor
public enum CrmCustomerOwnerRecordTypeEnum {

    PUT_POOL(1, "进入公海"),
    TAKE_POOL(2, "领取或分配"),
    INITIAL_ASSIGN(3, "初始分配"),
    TRANSFER(4, "转移"),
    PUT_GARBAGE(5, "转入垃圾池"),
    RESTORE_PUBLIC(6, "恢复到公海"),
    DELETE_GARBAGE(7, "永久删除垃圾客户");

    private final Integer type;
    private final String name;

}
