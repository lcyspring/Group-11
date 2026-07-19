package com.meession.etm.module.crm.enums.activity;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum CrmCallDirectionEnum {
    OUTBOUND(1),
    INBOUND(2);

    private final int direction;

    public static boolean contains(Integer value) {
        if (value == null) return false;
        for (CrmCallDirectionEnum item : values()) if (item.direction == value) return true;
        return false;
    }
}
