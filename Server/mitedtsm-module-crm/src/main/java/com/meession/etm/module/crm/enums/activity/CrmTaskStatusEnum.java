package com.meession.etm.module.crm.enums.activity;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum CrmTaskStatusEnum {
    NOT_STARTED(0),
    IN_PROGRESS(10),
    COMPLETED(20),
    NOT_COMPLETED(30),
    CANCELED(40),
    OVERDUE(50);

    private final int status;

    public boolean isTerminal() {
        return this == COMPLETED || this == NOT_COMPLETED || this == CANCELED;
    }

    public static CrmTaskStatusEnum valueOfStatus(Integer value) {
        if (value != null) for (CrmTaskStatusEnum item : values()) if (item.status == value) return item;
        return null;
    }
}
