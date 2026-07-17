package com.meession.etm.module.infra.enums.notification;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum NotificationTypeEnum {

    /**
     * 系统通知
     */
    SYSTEM(1),
    /**
     * 工作流通知
     */
    BPM(2),
    /**
     * 订单通知
     */
    ORDER(3),
    /**
     * 财务通知
     */
    FINANCE(4),
    /**
     * 营销通知
     */
    MARKETING(5);

    private final Integer type;

}
