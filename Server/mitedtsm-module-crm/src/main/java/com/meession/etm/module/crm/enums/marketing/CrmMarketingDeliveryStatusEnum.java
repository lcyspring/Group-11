package com.meession.etm.module.crm.enums.marketing;

import lombok.AllArgsConstructor;
import lombok.Getter;

/** 群发提供商结果，不与任务发送生命周期混用。 */
@Getter
@AllArgsConstructor
public enum CrmMarketingDeliveryStatusEnum {
    UNKNOWN(0),
    PROVIDER_PENDING(10),
    DELIVERED(20),
    FAILED(30),
    ACCEPTED(40);

    private final int status;
}
