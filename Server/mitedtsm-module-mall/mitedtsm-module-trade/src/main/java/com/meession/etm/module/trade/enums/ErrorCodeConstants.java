package com.meession.etm.module.trade.enums;

import com.meession.etm.framework.common.exception.ErrorCode;

public interface ErrorCodeConstants {

    ErrorCode ORDER_NOT_EXISTS = new ErrorCode(1_003_000_001, "订单不存在");
    ErrorCode ORDER_STATUS_ERROR = new ErrorCode(1_003_000_002, "订单状态错误");
    ErrorCode ORDER_ITEM_NOT_EXISTS = new ErrorCode(1_003_001_001, "订单项不存在");

}