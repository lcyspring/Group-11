package com.meession.etm.module.crm.enums.reimbursement;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum CrmReimbursementActionTypeEnum {
    CREATE(1), UPDATE(2), SUBMIT(3), APPROVE(4), REJECT(5), CANCEL(6), DELETE(7);

    private final Integer type;
}
