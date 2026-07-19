package com.meession.etm.module.crm.enums.quote;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum CrmQuoteStatusEnum {
    DRAFT(0, "草稿"),
    LOCKED(10, "已锁定"),
    SUPERSEDED(20, "已被新版本替代"),
    TERMINATED(30, "商机结束已终止");

    private final Integer status;
    private final String name;
}
