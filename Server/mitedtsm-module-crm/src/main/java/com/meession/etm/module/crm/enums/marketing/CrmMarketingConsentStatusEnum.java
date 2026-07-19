package com.meession.etm.module.crm.enums.marketing;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum CrmMarketingConsentStatusEnum {
    OPTED_IN(1), OPTED_OUT(2);
    private final Integer status;
}
