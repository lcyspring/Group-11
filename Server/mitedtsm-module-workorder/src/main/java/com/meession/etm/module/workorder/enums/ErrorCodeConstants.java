package com.meession.etm.module.workorder.enums;

import com.meession.etm.framework.common.exception.ErrorCode;

/**
 * 工单模块错误码枚举类
 * <p>
 * workorder 系统，使用 1-021-000-000 段
 *
 * @author fwx
 */
public interface ErrorCodeConstants {

    // ========== 工单管理 1-021-000-000 ==========
    ErrorCode WORK_ORDER_NOT_EXISTS = new ErrorCode(1_021_000_000, "工单不存在");
    ErrorCode WORK_ORDER_UPDATE_STATUS_FAIL = new ErrorCode(1_021_000_001, "更新工单状态失败，原因：不允许的状态流转");
    ErrorCode WORK_ORDER_UPDATE_STATUS_FAIL_STATUS_EQUALS = new ErrorCode(1_021_000_002, "更新工单状态失败，原因：已经是该状态");
    ErrorCode WORK_ORDER_UPDATE_STATUS_FAIL_END_STATUS = new ErrorCode(1_021_000_003, "更新工单状态失败，原因：工单已经结束，不允许变更");
    ErrorCode WORK_ORDER_DELETE_FAIL = new ErrorCode(1_021_000_004, "删除工单失败，原因：工单处于处理中状态，不允许删除");

    // ========== 工单类型管理 1-021-001-000 ==========
    ErrorCode WORK_ORDER_TYPE_NOT_EXISTS = new ErrorCode(1_021_001_000, "工单类型不存在");
    ErrorCode WORK_ORDER_TYPE_CODE_EXISTS = new ErrorCode(1_021_001_001, "工单类型编码已存在");
    ErrorCode WORK_ORDER_TYPE_NAME_EXISTS = new ErrorCode(1_021_001_002, "工单类型名称已存在");
    ErrorCode WORK_ORDER_TYPE_USED = new ErrorCode(1_021_001_003, "工单类型已被工单使用，无法删除");

}
