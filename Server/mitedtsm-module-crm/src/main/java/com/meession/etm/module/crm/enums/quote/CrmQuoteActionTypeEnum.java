package com.meession.etm.module.crm.enums.quote;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum CrmQuoteActionTypeEnum {
    CREATE(1), UPDATE(2), LOCK(3), REOPEN(4), TERMINATE(5);

    private final Integer type;
}
