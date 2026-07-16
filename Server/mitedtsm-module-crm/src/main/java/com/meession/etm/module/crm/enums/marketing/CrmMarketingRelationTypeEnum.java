package com.meession.etm.module.crm.enums.marketing;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Arrays;

@Getter
@RequiredArgsConstructor
public enum CrmMarketingRelationTypeEnum {
    CLUE(1), CUSTOMER(2), BUSINESS(3), TASK(4);

    private final Integer type;

    public static boolean isValid(Integer type) {
        return Arrays.stream(values()).anyMatch(item -> item.type.equals(type));
    }
}
