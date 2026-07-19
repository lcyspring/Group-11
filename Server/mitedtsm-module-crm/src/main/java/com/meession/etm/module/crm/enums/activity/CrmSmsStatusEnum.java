package com.meession.etm.module.crm.enums.activity;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum CrmSmsStatusEnum {
    PENDING(0),
    SENT(10),
    DELIVERED(20),
    FAILED(30),
    RECEIVED(40);

    private final int status;

    public static boolean contains(Integer value) {
        if (value == null) return false;
        for (CrmSmsStatusEnum item : values()) if (item.status == value) return true;
        return false;
    }
}
