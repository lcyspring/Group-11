package com.meession.etm.module.crm.enums.receivable;

import com.meession.etm.framework.common.core.ArrayValuable;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;

/**
 * CRM 回款客户、合同引用完整性状态。
 */
@Getter
@AllArgsConstructor
public enum CrmReceivableReferenceStatusEnum implements ArrayValuable<Integer> {

    VALID(0, "引用完整"),
    CUSTOMER_MISSING(10, "客户引用缺失"),
    CONTRACT_INVALID(20, "合同引用缺失或与客户不匹配"),
    BOTH_INVALID(30, "客户引用缺失且合同引用无效");

    public static final Integer[] ARRAYS = Arrays.stream(values())
            .map(CrmReceivableReferenceStatusEnum::getStatus).toArray(Integer[]::new);

    private final Integer status;
    private final String name;

    public static CrmReceivableReferenceStatusEnum resolve(boolean customerExists, boolean contractValid) {
        if (customerExists && contractValid) {
            return VALID;
        }
        if (!customerExists && contractValid) {
            return CUSTOMER_MISSING;
        }
        if (customerExists) {
            return CONTRACT_INVALID;
        }
        return BOTH_INVALID;
    }

    @Override
    public Integer[] array() {
        return ARRAYS;
    }

}
