package com.meession.etm.module.crm.enums.invoice;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum CrmInvoiceActionTypeEnum {

    CREATE(1, "创建草稿"),
    UPDATE(2, "修改草稿"),
    ISSUE(3, "正式开具"),
    VOID(4, "作废"),
    RED_FLUSH(5, "红冲"),
    VOID_RED(6, "作废红票"),
    DELETE(7, "删除草稿");

    private final Integer actionType;
    private final String name;
}
