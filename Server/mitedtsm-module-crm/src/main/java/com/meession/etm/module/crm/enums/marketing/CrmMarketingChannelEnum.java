package com.meession.etm.module.crm.enums.marketing;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum CrmMarketingChannelEnum {
    SMS(1), EMAIL(2), BOTH(3);
    private final Integer channel;
    public boolean includesSms() { return channel == 1 || channel == 3; }
    public boolean includesEmail() { return channel == 2 || channel == 3; }
}
