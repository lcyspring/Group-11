package com.meession.etm.module.crm.enums.activity;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum CrmTaskTypeEnum {
    NORMAL(1),
    FOLLOW_UP(2);

    private final int type;

    public static boolean contains(Integer value) {
        if (value == null) return false;
        for (CrmTaskTypeEnum item : values()) if (item.type == value) return true;
        return false;
    }
}
