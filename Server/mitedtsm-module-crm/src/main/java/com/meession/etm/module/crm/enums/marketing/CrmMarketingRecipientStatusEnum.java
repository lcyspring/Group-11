package com.meession.etm.module.crm.enums.marketing;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum CrmMarketingRecipientStatusEnum {
    PENDING(10), SENDING(15), SENT(20), FAILED(30), SUPPRESSED(40), RECORDED(50);
    private final Integer status;
}
