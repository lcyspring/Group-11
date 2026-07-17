package com.meession.etm.module.trade.enums.order;

import cn.hutool.core.util.ArrayUtil;
import com.meession.etm.framework.common.core.ArrayValuable;
import com.meession.etm.framework.common.util.object.ObjectUtils;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;

@Getter
@AllArgsConstructor
public enum TradeOrderApprovalStatusEnum implements ArrayValuable<Integer> {

    PENDING(0, "待审批"),
    APPROVED(1, "审批通过"),
    REJECTED(2, "审批不通过"),
    CANCELLED(3, "已取消");

    public static final Integer[] ARRAYS = Arrays.stream(values()).map(TradeOrderApprovalStatusEnum::getStatus).toArray(Integer[]::new);

    private final Integer status;
    private final String desc;

    @Override
    public Integer[] array() {
        return ARRAYS;
    }

    public static boolean isApprovedStatus(Integer status) {
        return APPROVED.getStatus().equals(status);
    }

    public static boolean isRejectedStatus(Integer status) {
        return REJECTED.getStatus().equals(status);
    }

    public static boolean isEndStatus(Integer status) {
        return ObjectUtils.equalsAny(status, APPROVED.getStatus(), REJECTED.getStatus(), CANCELLED.getStatus());
    }

    public static TradeOrderApprovalStatusEnum valueOf(Integer status) {
        return ArrayUtil.firstMatch(item -> item.getStatus().equals(status), values());
    }

}