package com.meession.etm.module.crm.enums.clue;

import lombok.AllArgsConstructor;
import lombok.Getter;

/** Explicit ownership state of an untransformed clue. */
@Getter
@AllArgsConstructor
public enum CrmCluePoolStatusEnum {

    OWNED(0, "在管"),
    PUBLIC(1, "公共线索");

    private final Integer status;
    private final String name;
}
