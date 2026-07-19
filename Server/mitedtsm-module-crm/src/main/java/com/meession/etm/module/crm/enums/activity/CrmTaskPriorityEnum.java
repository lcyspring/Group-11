package com.meession.etm.module.crm.enums.activity;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum CrmTaskPriorityEnum {
    LOW(1),
    MEDIUM(2),
    HIGH(3);

    private final int priority;

    public static boolean contains(Integer value) {
        if (value == null) return false;
        for (CrmTaskPriorityEnum item : values()) if (item.priority == value) return true;
        return false;
    }
}
