package com.meession.etm.module.crm.enums.marketing;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum CrmMarketingCampaignStatusEnum {
    DRAFT(10), ACTIVE(20), LOCKED(30), TERMINATED(40), COMPLETED(50);

    private final Integer status;
}
