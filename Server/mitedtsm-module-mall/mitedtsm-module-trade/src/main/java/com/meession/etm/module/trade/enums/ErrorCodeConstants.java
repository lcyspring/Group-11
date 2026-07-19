package com.meession.etm.module.trade.enums;

import com.meession.etm.framework.common.exception.ErrorCode;

/**
 * Trade 模块错误码
 */
public interface ErrorCodeConstants {

    // ========== 订单相关 1003000000 ==========
    ErrorCode ORDER_NOT_EXISTS = new ErrorCode(1003000001, "订单不存在");
    ErrorCode ORDER_ITEM_NOT_EXISTS = new ErrorCode(1003000002, "订单项不存在");
    ErrorCode ORDER_STATUS_ERROR = new ErrorCode(1003000003, "订单状态不正确");
    ErrorCode ORDER_CANNOT_UPDATE = new ErrorCode(1003000004, "订单无法修改");
    ErrorCode ORDER_CANNOT_DELETE = new ErrorCode(1003000005, "订单无法删除");
    ErrorCode ORDER_CANNOT_CANCEL = new ErrorCode(1003000006, "订单无法取消");
    ErrorCode ORDER_ITEM_COUNT_ERROR = new ErrorCode(1003000007, "订单项数量错误");
    ErrorCode ORDER_STATUS_CANNOT_TRANSITION = new ErrorCode(1003000008, "订单状态不允许转换");
    ErrorCode ORDER_PAY_PRICE_NOT_MATCH = new ErrorCode(1003000009, "支付金额不匹配");
    ErrorCode ORDER_PAY_PRICE_NOT_ENOUGH = new ErrorCode(1003000010, "支付金额不足");

    // ========== 合同相关 1003001000 ==========
    ErrorCode CONTRACT_NOT_EXISTS = new ErrorCode(1003001001, "合同不存在");
    ErrorCode CONTRACT_STATUS_ERROR = new ErrorCode(1003001002, "合同状态不正确");
    ErrorCode CONTRACT_CANNOT_UPDATE = new ErrorCode(1003001003, "合同无法修改");
    ErrorCode CONTRACT_CANNOT_DELETE = new ErrorCode(1003001004, "合同无法删除");
    ErrorCode CONTRACT_CANNOT_SIGN = new ErrorCode(1003001005, "合同无法签署");

    // ========== 商机相关 1003002000 ==========
    ErrorCode OPPORTUNITY_NOT_EXISTS = new ErrorCode(1003002001, "商机不存在");
    ErrorCode OPPORTUNITY_ITEM_NOT_EXISTS = new ErrorCode(1003002002, "商机项不存在");
    ErrorCode OPPORTUNITY_STATUS_ERROR = new ErrorCode(1003002003, "商机状态不正确");
    ErrorCode OPPORTUNITY_CANNOT_CONVERT = new ErrorCode(1003002004, "商机无法转换为订单");

    // ========== 支付相关 1003003000 ==========
    ErrorCode PAYMENT_NOT_EXISTS = new ErrorCode(1003003001, "支付记录不存在");
    ErrorCode PAYMENT_STATUS_ERROR = new ErrorCode(1003003002, "支付状态不正确");
    ErrorCode PAYMENT_AMOUNT_ERROR = new ErrorCode(1003003003, "支付金额错误");
    ErrorCode REFUND_NOT_EXISTS = new ErrorCode(1003003004, "退款记录不存在");
    ErrorCode REFUND_AMOUNT_ERROR = new ErrorCode(1003003005, "退款金额错误");

}