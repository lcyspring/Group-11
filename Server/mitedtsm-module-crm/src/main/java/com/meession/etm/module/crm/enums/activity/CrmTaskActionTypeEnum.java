package com.meession.etm.module.crm.enums.activity;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum CrmTaskActionTypeEnum {
    CREATE(1),
    UPDATE(2),
    START(3),
    COMPLETE(4),
    UNABLE(5),
    CANCEL(6),
    OVERDUE(7),
    MIGRATE(8);

    private final int type;
}
