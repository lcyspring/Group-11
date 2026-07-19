package com.meession.etm.module.crm.enums.customer;

import lombok.AllArgsConstructor;
import lombok.Getter;

/** Source/reason code of an immutable customer ownership event. */
@Getter
@AllArgsConstructor
public enum CrmCustomerOwnerRecordSourceEnum {

    INITIAL_ASSIGN("INITIAL_ASSIGN"),
    TRANSFER("TRANSFER"),
    MANUAL_PUT_POOL("MANUAL_PUT_POOL"),
    AUTO_NO_FOLLOW_UP("AUTO_NO_FOLLOW_UP"),
    AUTO_NO_DEAL("AUTO_NO_DEAL"),
    IMPORT_UNASSIGNED("IMPORT_UNASSIGNED"),
    SELF_CLAIM("SELF_CLAIM"),
    MANAGER_ASSIGN("MANAGER_ASSIGN"),
    MANUAL_GARBAGE("MANUAL_GARBAGE"),
    AUTO_GARBAGE("AUTO_GARBAGE"),
    RESTORE_PUBLIC("RESTORE_PUBLIC");

    private final String source;
}
