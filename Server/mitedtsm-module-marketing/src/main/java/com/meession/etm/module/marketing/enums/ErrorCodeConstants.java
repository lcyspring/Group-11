package com.meession.etm.module.marketing.enums;

import com.meession.etm.framework.common.exception.ErrorCode;

/**
 * 营销模块错误码枚举类
 * <p>
 * marketing 系统，使用 1-030-000-000 段
 */
public interface ErrorCodeConstants {

    // ========== 营销活动 1-030-000-000 ==========
    ErrorCode CAMPAIGN_NOT_EXISTS = new ErrorCode(1_030_000_000, "营销活动不存在");
    ErrorCode CAMPAIGN_STATUS_NOT_DRAFT = new ErrorCode(1_030_000_001, "营销活动不是草稿状态，无法操作");
    ErrorCode CAMPAIGN_STATUS_NOT_APPROVED = new ErrorCode(1_030_000_002, "营销活动未通过审核，无法启动");
    ErrorCode CAMPAIGN_NAME_DUPLICATE = new ErrorCode(1_030_000_003, "已存在名为【{}】的营销活动");
    ErrorCode CAMPAIGN_SUBMIT_FAIL = new ErrorCode(1_030_000_004, "营销活动提交审核失败");
    ErrorCode CAMPAIGN_ALREADY_STARTED = new ErrorCode(1_030_000_005, "营销活动已启动，不能重复启动");
    ErrorCode CAMPAIGN_TEMPLATE_NOT_EXISTS = new ErrorCode(1_030_000_006, "营销活动关联的模板不存在");

    // ========== 短信模板(营销) 1-030-001-000 ==========
    ErrorCode MARKETING_SMS_TEMPLATE_NOT_EXISTS = new ErrorCode(1_030_001_000, "短信模板不存在或已禁用");

    // ========== 邮件模板(营销) 1-030-002-000 ==========
    ErrorCode MARKETING_MAIL_TEMPLATE_NOT_EXISTS = new ErrorCode(1_030_002_000, "邮件模板不存在或已禁用");

    // ========== 批量发送 1-030-003-000 ==========
    ErrorCode BATCH_SEND_MOBILE_LIST_EMPTY = new ErrorCode(1_030_003_000, "批量发送的手机号列表不能为空");
    ErrorCode BATCH_SEND_MAIL_LIST_EMPTY = new ErrorCode(1_030_003_001, "批量发送的邮箱列表不能为空");
    ErrorCode BATCH_SEND_TEMPLATE_CODE_EMPTY = new ErrorCode(1_030_003_002, "批量发送的模板编码不能为空");

}
