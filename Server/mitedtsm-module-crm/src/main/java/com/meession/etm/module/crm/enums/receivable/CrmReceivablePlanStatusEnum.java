package com.meession.etm.module.crm.enums.receivable;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * CRM 回款计划执行状态枚举。
 */
@Getter
@AllArgsConstructor
public enum CrmReceivablePlanStatusEnum {

    PENDING(0, "待回款"),
    OVERDUE(10, "已逾期"),
    RECEIVED(20, "已回款");

    private final Integer status;
    private final String name;

}
