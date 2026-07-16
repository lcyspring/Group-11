package com.meession.etm.module.crm.enums.marketing;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Arrays;

@Getter
@RequiredArgsConstructor
public enum CrmCustomerCareRuleTypeEnum {
    BIRTHDAY(1), HOLIDAY(2), POST_DEAL_FOLLOW_UP(3);

    private final Integer type;

    public static boolean isValid(Integer type) {
        return type != null && Arrays.stream(values()).anyMatch(item -> item.type.equals(type));
    }
}
