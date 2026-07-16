package com.meession.etm.module.crm.enums.contract;

public final class CrmContractLifecycleEnums {

    private CrmContractLifecycleEnums() {
    }

    public static final int ATTACHMENT_GENERAL = 1;
    public static final int ATTACHMENT_SIGNED_COPY = 2;
    public static final int ATTACHMENT_AMENDMENT = 3;
    public static final int SIGNED = 10;
    public static final int SIGN_VOIDED = 20;
    public static final int SIGN_OFFLINE = 1;
    public static final int SIGN_ELECTRONIC = 2;
    public static final int ACTION_CREATE = 1;
    public static final int ACTION_UPDATE = 2;
    public static final int ACTION_SUBMIT = 3;
    public static final int ACTION_APPROVE = 4;
    public static final int ACTION_REJECT = 5;
    public static final int ACTION_CANCEL = 6;
    public static final int ACTION_SIGN = 7;
    public static final int ACTION_VOID_SIGN = 8;
    public static final int ACTION_AMENDMENT_CREATE = 9;
    public static final int ACTION_AMENDMENT_UPDATE = 10;
    public static final int ACTION_AMENDMENT_SUBMIT = 11;
    public static final int ACTION_AMENDMENT_EFFECTIVE = 12;
    public static final int ACTION_AMENDMENT_REJECT = 13;
    public static final int ACTION_AMENDMENT_CANCEL = 14;
}
