package com.meession.etm.module.crm.enums.workorder;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum CrmWorkOrderDispatchModeEnum {
    UNASSIGNED(0), MANUAL(1), AUTO(2), CLAIM(3), REASSIGN(4);

    private final Integer mode;
}
