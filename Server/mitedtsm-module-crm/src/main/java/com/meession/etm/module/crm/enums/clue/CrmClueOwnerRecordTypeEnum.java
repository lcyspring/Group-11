package com.meession.etm.module.crm.enums.clue;

import lombok.AllArgsConstructor;
import lombok.Getter;

/** Immutable clue ownership event type. */
@Getter
@AllArgsConstructor
public enum CrmClueOwnerRecordTypeEnum {

    PUT_POOL(1, "进入公共线索"),
    TAKE_POOL(2, "领取或分配"),
    INITIAL_ASSIGN(3, "初始分配"),
    TRANSFER(4, "转移");

    private final Integer type;
    private final String name;
}
