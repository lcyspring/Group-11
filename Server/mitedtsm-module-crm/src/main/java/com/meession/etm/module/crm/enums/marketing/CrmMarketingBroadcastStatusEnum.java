package com.meession.etm.module.crm.enums.marketing;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum CrmMarketingBroadcastStatusEnum {
    DRAFT(10), PENDING_REVIEW(20), REJECTED(30), READY(40), SENDING(50), SENT(60), PARTIAL_FAILED(70), CANCELLED(80);
    private final Integer status;
}
