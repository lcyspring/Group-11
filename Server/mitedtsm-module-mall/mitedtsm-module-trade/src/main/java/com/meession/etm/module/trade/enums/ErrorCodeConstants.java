package com.meession.etm.module.trade.enums;

import com.meession.etm.framework.common.exception.ErrorCode;

public interface ErrorCodeConstants {

    ErrorCode ORDER_NOT_EXISTS = new ErrorCode(1_003_000_001, "订单不存在");
    ErrorCode ORDER_STATUS_ERROR = new ErrorCode(1_003_000_002, "订单状态错误");
    ErrorCode ORDER_STATUS_CANNOT_TRANSITION = new ErrorCode(1_003_000_003, "订单状态不允许此转换");
    ErrorCode ORDER_ITEM_NOT_EXISTS = new ErrorCode(1_003_001_001, "订单项不存在");
    ErrorCode OPPORTUNITY_NOT_EXISTS = new ErrorCode(1_003_002_001, "商机不存在");
    ErrorCode CONTRACT_NOT_EXISTS = new ErrorCode(1_003_003_001, "合同不存在");

}