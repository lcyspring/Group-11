package com.meession.etm.module.crm.enums.clue;

import lombok.AllArgsConstructor;
import lombok.Getter;

/** Machine-readable source of an immutable clue ownership event. */
@Getter
@AllArgsConstructor
public enum CrmClueOwnerRecordSourceEnum {

    INITIAL_ASSIGN("INITIAL_ASSIGN"),
    TRANSFER("TRANSFER"),
    MANUAL_PUT_POOL("MANUAL_PUT_POOL"),
    AUTO_NO_FOLLOW_UP("AUTO_NO_FOLLOW_UP"),
    CREATE_UNASSIGNED("CREATE_UNASSIGNED"),
    SELF_CLAIM("SELF_CLAIM"),
    MANAGER_ASSIGN("MANAGER_ASSIGN");

    private final String source;
}
