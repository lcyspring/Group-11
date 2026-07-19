package com.meession.etm.module.crm.enums.activity;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum CrmCallStatusEnum {
    CONNECTED(10),
    MISSED(20),
    FAILED(30);

    private final int status;

    public static boolean contains(Integer value) {
        if (value == null) return false;
        for (CrmCallStatusEnum item : values()) if (item.status == value) return true;
        return false;
    }
}
