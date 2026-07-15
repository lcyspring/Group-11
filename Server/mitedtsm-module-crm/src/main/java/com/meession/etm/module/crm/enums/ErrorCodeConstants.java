package com.meession.etm.module.crm.enums;

import com.meession.etm.framework.common.exception.ErrorCode;

/**
 * CRM 错误码枚举类
 * <p>
 * crm 系统，使用 1-020-000-000 段
 */
public interface ErrorCodeConstants {

    // ========== 合同管理 1-020-000-000 ==========
    ErrorCode CONTRACT_NOT_EXISTS = new ErrorCode(1_020_000_000, "合同不存在");
    ErrorCode CONTRACT_UPDATE_FAIL_NOT_EDITABLE = new ErrorCode(1_020_000_001,
            "合同更新失败，只有草稿、审核不通过或已取消状态可以编辑");
    ErrorCode CONTRACT_SUBMIT_FAIL_NOT_DRAFT = new ErrorCode(1_020_000_002, "合同提交审核失败，原因：合同没处在未提交状态");
    ErrorCode CONTRACT_UPDATE_AUDIT_STATUS_FAIL_NOT_PROCESS = new ErrorCode(1_020_000_003, "更新合同审核状态失败，原因：合同不是审核中状态");
    ErrorCode CONTRACT_NO_EXISTS = new ErrorCode(1_020_000_004, "生成合同序列号重复，请重试");
    ErrorCode CONTRACT_DELETE_FAIL = new ErrorCode(1_020_000_005, "删除合同失败，原因：有被回款所使用");
    ErrorCode CONTRACT_CREATE_FAIL_BUSINESS_NOT_WON = new ErrorCode(1_020_000_006, "只有已赢单商机才能创建合同");
    ErrorCode CONTRACT_CREATE_FROM_BUSINESS_CONCURRENT = new ErrorCode(1_020_000_007,
            "商机转合同状态已变化，请刷新后重试");
    ErrorCode CONTRACT_CREATE_BUSINESS_REQUIRES_CONVERSION = new ErrorCode(1_020_000_008,
            "关联合同必须通过商机转合同入口创建");
    ErrorCode CONTRACT_DELETE_FAIL_NOT_NEW_DRAFT = new ErrorCode(1_020_000_009,
            "删除合同失败，只有从未提交审批的草稿可以删除");
    ErrorCode CONTRACT_PRODUCT_ROW_NOT_BELONGS = new ErrorCode(1_020_000_010,
            "合同产品行不存在或不属于当前合同，请刷新后重试");
    ErrorCode CONTRACT_PRODUCT_ROW_DUPLICATE = new ErrorCode(1_020_000_011,
            "合同产品行重复，请刷新后重试");
    ErrorCode CONTRACT_SIGN_CONTACT_CUSTOMER_MISMATCH = new ErrorCode(1_020_000_012,
            "签约联系人不属于合同客户");
    ErrorCode CONTRACT_SIGN_REQUIRES_APPROVED = new ErrorCode(1_020_000_013, "只有审批通过的合同可以签署");
    ErrorCode CONTRACT_SIGN_ALREADY_EXISTS = new ErrorCode(1_020_000_014, "合同已经存在签署记录，不能覆盖");
    ErrorCode CONTRACT_SIGN_NOT_EXISTS = new ErrorCode(1_020_000_015, "合同签署记录不存在");
    ErrorCode CONTRACT_SIGN_STATUS_INVALID = new ErrorCode(1_020_000_016, "当前合同签署状态不允许该操作");
    ErrorCode CONTRACT_ATTACHMENT_NOT_EXISTS = new ErrorCode(1_020_000_017, "合同附件不存在");
    ErrorCode CONTRACT_ATTACHMENT_NOT_BELONGS = new ErrorCode(1_020_000_018, "附件不属于当前合同");
    ErrorCode CONTRACT_ATTACHMENT_SIGNED_COPY_REQUIRED = new ErrorCode(1_020_000_019, "签署必须选择签署副本附件");
    ErrorCode CONTRACT_ATTACHMENT_IMMUTABLE = new ErrorCode(1_020_000_020, "正式合同附件已锁定，不能删除");
    ErrorCode CONTRACT_SIGN_PROVIDER_INVALID = new ErrorCode(1_020_000_021, "合同签署适配器返回结果无效");
    ErrorCode CONTRACT_SIGN_METHOD_UNSUPPORTED = new ErrorCode(1_020_000_022, "当前签署适配器不支持所选签署方式");
    ErrorCode CONTRACT_ATTACHMENT_FILE_NOT_MANAGED = new ErrorCode(1_020_000_023,
            "合同附件必须来自系统受管文件存储");
    ErrorCode CONTRACT_ATTACHMENT_FILE_NOT_PROTECTED = new ErrorCode(1_020_000_024,
            "合同附件必须通过受保护的合同文件入口上传");

    // ========== 线索管理 1-020-001-000 ==========
    ErrorCode CLUE_NOT_EXISTS = new ErrorCode(1_020_001_000, "线索不存在");
    ErrorCode CLUE_TRANSFORM_FAIL_ALREADY = new ErrorCode(1_020_001_001, "线索已经转化过了，请勿重复转化");
    ErrorCode CLUE_UPDATE_FAIL_TRANSFORMED = new ErrorCode(1_020_001_002, "线索已转化为客户，只能查看");

    // ========== 商机管理 1-020-002-000 ==========
    ErrorCode BUSINESS_NOT_EXISTS = new ErrorCode(1_020_002_000, "商机不存在");
    ErrorCode BUSINESS_DELETE_FAIL_CONTRACT_EXISTS = new ErrorCode(1_020_002_001, "商机已关联合同，不能删除");
    ErrorCode BUSINESS_UPDATE_STATUS_FAIL_END_STATUS = new ErrorCode(1_020_002_002, "更新商机状态失败，原因：已经是结束状态");
    ErrorCode BUSINESS_UPDATE_STATUS_FAIL_STATUS_EQUALS = new ErrorCode(1_020_002_003, "更新商机状态失败，原因：已经是该状态");
    ErrorCode BUSINESS_UPDATE_STATUS_CONCURRENT = new ErrorCode(1_020_002_004, "商机状态已被其他操作修改，请刷新后重试");
    ErrorCode BUSINESS_UPDATE_STATUS_BACKWARD = new ErrorCode(1_020_002_005, "商机阶段只能向前推进，不能回退");

    // ========== 联系人管理 1-020-003-000 ==========
    ErrorCode CONTACT_NOT_EXISTS = new ErrorCode(1_020_003_000, "联系人不存在");
    ErrorCode CONTACT_DELETE_FAIL_CONTRACT_LINK_EXISTS = new ErrorCode(1_020_003_002, "联系人已关联合同，不能删除");
    ErrorCode CONTACT_UPDATE_OWNER_USER_FAIL = new ErrorCode(1_020_003_003, "更新联系人负责人失败");
    ErrorCode CONTACT_PRIMARY_UNSET_FAIL = new ErrorCode(1_020_003_004, "当前首联系人不能直接取消，请将其他联系人设为首联系人");
    ErrorCode CONTACT_PRIMARY_DELETE_FAIL = new ErrorCode(1_020_003_005, "当前首联系人不能删除，请先将其他联系人设为首联系人");
    ErrorCode CONTACT_PRIMARY_MOVE_FAIL = new ErrorCode(1_020_003_006, "当前首联系人不能移动到其他客户，请先将其他联系人设为首联系人");
    ErrorCode CONTACT_PRIMARY_SWITCH_CONFLICT = new ErrorCode(1_020_003_007, "首联系人状态已变化，请重试");
    ErrorCode CONTACT_MOBILE_EXISTS = new ErrorCode(1_020_003_008, "该客户下已存在手机号为【{}】的联系人");
    ErrorCode CONTACT_CONCURRENT_CHANGE = new ErrorCode(1_020_003_009, "联系人状态已变化，请重试");

    // ========== 回款 1-020-004-000 ==========
    ErrorCode RECEIVABLE_NOT_EXISTS = new ErrorCode(1_020_004_000, "回款不存在");
    ErrorCode RECEIVABLE_UPDATE_FAIL_EDITING_PROHIBITED = new ErrorCode(1_020_004_001, "更新回款失败，原因：禁止编辑");
    ErrorCode RECEIVABLE_DELETE_FAIL = new ErrorCode(1_020_004_002, "删除回款失败，原因： 被回款计划所使用，不允许删除");
    ErrorCode RECEIVABLE_SUBMIT_FAIL_NOT_DRAFT = new ErrorCode(1_020_004_003, "回款提交审核失败，原因：回款没处在未提交状态");
    ErrorCode RECEIVABLE_UPDATE_AUDIT_STATUS_FAIL_NOT_PROCESS = new ErrorCode(1_020_004_004, "更新回款审核状态失败，原因：回款不是审核中状态");
    ErrorCode RECEIVABLE_NO_EXISTS = new ErrorCode(1_020_004_005, "生成回款序列号重复，请重试");
    ErrorCode RECEIVABLE_CREATE_FAIL_CONTRACT_NOT_APPROVE = new ErrorCode(1_020_004_006, "创建回款失败，原因：合同不是审核通过状态");
    ErrorCode RECEIVABLE_CREATE_FAIL_PRICE_EXCEEDS_LIMIT = new ErrorCode(1_020_004_007,
            "创建回款失败，原因：回款金额超出合同金额，目前剩余可回款：{} 元");
    ErrorCode RECEIVABLE_DELETE_FAIL_IS_APPROVE = new ErrorCode(1_020_004_008, "删除回款失败，原因：回款审批已通过");
    ErrorCode RECEIVABLE_DELETE_FAIL_NOT_NEW_DRAFT = new ErrorCode(1_020_004_009,
            "删除回款失败，只有从未提交审批且未关联计划的草稿可以删除");

    // ========== 回款计划 1-020-005-000 ==========
    ErrorCode RECEIVABLE_PLAN_NOT_EXISTS = new ErrorCode(1_020_005_000, "回款计划不存在");
    ErrorCode RECEIVABLE_PLAN_UPDATE_FAIL = new ErrorCode(1_020_006_000,
            "更新回款计划失败，原因：已经有关联回款");
    ErrorCode RECEIVABLE_PLAN_EXISTS_RECEIVABLE = new ErrorCode(1_020_006_001, "回款计划已经有对应的回款，不能使用");
    ErrorCode RECEIVABLE_PLAN_CREATE_FAIL_CONTRACT_NOT_APPROVE = new ErrorCode(1_020_005_001,
            "创建回款计划失败，原因：合同不是审核通过状态");
    ErrorCode RECEIVABLE_PLAN_PRICE_EXCEEDS_CONTRACT = new ErrorCode(1_020_005_002,
            "保存回款计划失败，计划总额超出合同金额，目前剩余可计划：{} 元");
    ErrorCode RECEIVABLE_PLAN_DELETE_FAIL_LINKED = new ErrorCode(1_020_005_003,
            "删除回款计划失败，原因：已经有关联回款");

    // ========== 客户管理 1_020_006_000 ==========
    ErrorCode CUSTOMER_NOT_EXISTS = new ErrorCode(1_020_006_000, "客户不存在");
    ErrorCode CUSTOMER_OWNER_EXISTS = new ErrorCode(1_020_006_001, "客户【{}】已存在所属负责人");
    ErrorCode CUSTOMER_LOCKED = new ErrorCode(1_020_006_002, "客户【{}】状态已锁定");
    ErrorCode CUSTOMER_ALREADY_DEAL = new ErrorCode(1_020_006_003, "客户已交易");
    ErrorCode CUSTOMER_IN_POOL = new ErrorCode(1_020_006_004, "客户【{}】放入公海失败，原因：已经是公海客户");
    ErrorCode CUSTOMER_LOCKED_PUT_POOL_FAIL = new ErrorCode(1_020_006_005, "客户【{}】放入公海失败，原因：客户已锁定");
    ErrorCode CUSTOMER_UPDATE_OWNER_USER_FAIL = new ErrorCode(1_020_006_006, "更新客户【{}】负责人失败, 原因：系统异常");
    ErrorCode CUSTOMER_LOCK_FAIL_IS_LOCK = new ErrorCode(1_020_006_007, "锁定客户失败，它已经处于锁定状态");
    ErrorCode CUSTOMER_UNLOCK_FAIL_IS_UNLOCK = new ErrorCode(1_020_006_008, "解锁客户失败，它已经处于未锁定状态");
    ErrorCode CUSTOMER_LOCK_EXCEED_LIMIT = new ErrorCode(1_020_006_009, "锁定客户失败，超出锁定规则上限");
    ErrorCode CUSTOMER_OWNER_EXCEED_LIMIT = new ErrorCode(1_020_006_010, "操作失败，超出客户数拥有上限");
    ErrorCode CUSTOMER_DELETE_FAIL_HAVE_REFERENCE = new ErrorCode(1_020_006_011, "删除客户失败，有关联{}");
    ErrorCode CUSTOMER_IMPORT_LIST_IS_EMPTY = new ErrorCode(1_020_006_012, "导入客户数据不能为空！");
    ErrorCode CUSTOMER_CREATE_NAME_NOT_NULL = new ErrorCode(1_020_006_013, "客户名称不能为空！");
    ErrorCode CUSTOMER_NAME_EXISTS = new ErrorCode(1_020_006_014, "已存在名为【{}】的客户！");
    ErrorCode CUSTOMER_UPDATE_DEAL_STATUS_FAIL = new ErrorCode(1_020_006_015, "更新客户的成交状态失败，原因：已经是该状态，无需更新");
    ErrorCode CUSTOMER_PARENT_SELF = new ErrorCode(1_020_006_016, "上级客户不能是客户自身");
    ErrorCode CUSTOMER_PARENT_NOT_EXISTS = new ErrorCode(1_020_006_017, "上级客户不存在或不属于当前租户");
    ErrorCode CUSTOMER_HIERARCHY_CYCLE = new ErrorCode(1_020_006_018, "客户上下级关系不能形成循环");
    ErrorCode CUSTOMER_LIFECYCLE_STATUS_SAME = new ErrorCode(1_020_006_019, "客户已经处于目标生命周期状态，无需重复更新");
    ErrorCode CUSTOMER_LIFECYCLE_LOST_REASON_REQUIRED = new ErrorCode(1_020_006_020, "客户转为流失状态时必须填写流失原因");
    ErrorCode CUSTOMER_LIFECYCLE_STATUS_INVALID = new ErrorCode(1_020_006_021, "无效的客户生命周期状态：{}");

    // ========== 权限管理 1_020_007_000 ==========
    ErrorCode CRM_PERMISSION_NOT_EXISTS = new ErrorCode(1_020_007_000, "数据权限不存在");
    ErrorCode CRM_PERMISSION_DENIED = new ErrorCode(1_020_007_001, "{}操作失败，原因：没有权限");
    ErrorCode CRM_PERMISSION_MODEL_TRANSFER_FAIL_OWNER_USER_EXISTS = new ErrorCode(1_020_007_003, "{}操作失败，原因：转移对象已经是该负责人");
    ErrorCode CRM_PERMISSION_DELETE_FAIL = new ErrorCode(1_020_007_004, "删除数据权限失败，原因：批量删除权限的时候，只能属于同一个 bizId 下");
    ErrorCode CRM_PERMISSION_DELETE_DENIED = new ErrorCode(1_020_007_006, "删除数据权限失败，原因：没有权限");
    ErrorCode CRM_PERMISSION_DELETE_SELF_PERMISSION_FAIL_EXIST_OWNER = new ErrorCode(1_020_007_007, "删除数据权限失败，原因：不能删除负责人");
    ErrorCode CRM_PERMISSION_CREATE_FAIL = new ErrorCode(1_020_007_008, "创建数据权限失败，原因：所加用户已有权限");
    ErrorCode CRM_PERMISSION_CREATE_FAIL_EXISTS = new ErrorCode(1_020_007_009, "同时添加数据权限失败，原因：用户【{}】已有模块【{}】数据【{}】的【{}】权限");
    ErrorCode CRM_EXPORT_PERMISSION_DENIED = new ErrorCode(1_020_007_010,
            "导出{}失败：所选数据包含仅可查看或无权导出的对象");

    // ========== 产品 1_020_008_000 ==========
    ErrorCode PRODUCT_NOT_EXISTS = new ErrorCode(1_020_008_000, "产品不存在");
    ErrorCode PRODUCT_NO_EXISTS = new ErrorCode(1_020_008_001, "产品编号已存在");
    ErrorCode PRODUCT_NOT_ENABLE = new ErrorCode(1_020_008_002, "产品【{}】已禁用");

    // ========== 产品分类 1_020_009_000 ==========
    ErrorCode PRODUCT_CATEGORY_NOT_EXISTS = new ErrorCode(1_020_009_000, "产品分类不存在");
    ErrorCode PRODUCT_CATEGORY_EXISTS = new ErrorCode(1_020_009_001, "产品分类已存在");
    ErrorCode PRODUCT_CATEGORY_USED = new ErrorCode(1_020_009_002, "产品分类已关联产品");
    ErrorCode PRODUCT_CATEGORY_PARENT_NOT_EXISTS = new ErrorCode(1_020_009_003, "父分类不存在");
    ErrorCode PRODUCT_CATEGORY_PARENT_NOT_FIRST_LEVEL = new ErrorCode(1_020_009_004, "父分类不能是二级分类");
    ErrorCode PRODUCT_CATEGORY_EXISTS_CHILDREN = new ErrorCode(1_020_009_005, "存在子分类，无法删除");

    // ========== 商机状态 1_020_010_000 ==========
    ErrorCode BUSINESS_STATUS_TYPE_NOT_EXISTS = new ErrorCode(1_020_010_000, "商机状态组不存在");
    ErrorCode BUSINESS_STATUS_TYPE_NAME_EXISTS = new ErrorCode(1_020_010_001, "商机状态组的名称已存在");
    ErrorCode BUSINESS_STATUS_UPDATE_FAIL_USED = new ErrorCode(1_020_010_002, "已经被使用的商机状态组，无法进行更新");
    ErrorCode BUSINESS_STATUS_DELETE_FAIL_USED = new ErrorCode(1_020_010_002, "已经被使用的商机状态组，无法进行删除");
    ErrorCode BUSINESS_STATUS_NOT_EXISTS = new ErrorCode(1_020_010_003, "商机状态不存在");

    // ========== 客户公海规则设置 1_020_012_000 ==========
    ErrorCode CUSTOMER_LIMIT_CONFIG_NOT_EXISTS = new ErrorCode(1_020_012_001, "客户限制配置不存在");

    // ========== 跟进记录 1_020_013_000 ==========
    ErrorCode FOLLOW_UP_RECORD_NOT_EXISTS = new ErrorCode(1_020_013_000, "跟进记录不存在");
    ErrorCode FOLLOW_UP_RECORD_DELETE_DENIED = new ErrorCode(1_020_013_001, "删除跟进记录失败，原因：没有权限");

    // ========== 数据统计 1_020_014_000 ==========
    ErrorCode PERFORMANCE_TARGET_SCOPE_INVALID = new ErrorCode(1_020_014_000, "业绩目标范围无效");
    ErrorCode PERFORMANCE_TARGET_COUNT_DECIMAL = new ErrorCode(1_020_014_001, "计数类业绩目标必须是整数");
    ErrorCode PERFORMANCE_TARGET_CONCURRENT_MODIFICATION = new ErrorCode(1_020_014_002,
            "业绩目标已被其他操作修改，请刷新后重试");
    ErrorCode PERFORMANCE_TARGET_TYPE_INVALID = new ErrorCode(1_020_014_003, "业绩目标类型无效");
    ErrorCode PERFORMANCE_TARGET_FILTER_SCOPE_MISMATCH = new ErrorCode(1_020_014_004,
            "业绩目标范围与部门、人员筛选条件不一致");
    ErrorCode PERFORMANCE_TARGET_PERIOD_INVALID = new ErrorCode(1_020_014_005,
            "业绩目标完成度必须查询同一个完整年度");

    // ========== 客服工单 1_020_015_000 ==========
    ErrorCode WORK_ORDER_NOT_EXISTS = new ErrorCode(1_020_015_000, "客服工单不存在");
    ErrorCode WORK_ORDER_STATUS_TRANSITION_INVALID = new ErrorCode(1_020_015_001,
            "客服工单状态已变化或不允许执行当前操作，请刷新后重试");
    ErrorCode WORK_ORDER_CREATOR_ONLY = new ErrorCode(1_020_015_002, "只有工单创建人可以执行当前操作");
    ErrorCode WORK_ORDER_HANDLER_ONLY = new ErrorCode(1_020_015_003, "只有工单处理人可以执行当前操作");
    ErrorCode WORK_ORDER_SOURCE_CUSTOMER_MISMATCH = new ErrorCode(1_020_015_004,
            "工单来源与所选客户不一致");
    ErrorCode WORK_ORDER_SOLUTION_REQUIRED = new ErrorCode(1_020_015_005, "完结工单必须填写解决方案");
    ErrorCode WORK_ORDER_NO_EXISTS = new ErrorCode(1_020_015_006, "生成客服工单编号重复，请重试");
    ErrorCode WORK_ORDER_DELETE_STATUS_INVALID = new ErrorCode(1_020_015_007,
            "只有创建人可以删除本人创建且待处理的工单");
    ErrorCode WORK_ORDER_QUERY_DENIED = new ErrorCode(1_020_015_008, "无权查看该客服工单");
    ErrorCode WORK_ORDER_ASSIGN_DENIED = new ErrorCode(1_020_015_009,
            "只有工单创建人或具有全量工单权限的调度人员可以分派工单");
    ErrorCode WORK_ORDER_HANDLER_UNCHANGED = new ErrorCode(1_020_015_010, "新处理人不能与当前处理人相同");

    // ========== 发票 1_020_016_000 ==========
    ErrorCode INVOICE_NOT_EXISTS = new ErrorCode(1_020_016_000, "发票不存在");
    ErrorCode INVOICE_NO_EXISTS = new ErrorCode(1_020_016_001, "发票申请号已存在，请重试");
    ErrorCode INVOICE_FISCAL_NO_EXISTS = new ErrorCode(1_020_016_002, "税务发票号码已存在");
    ErrorCode INVOICE_CONTRACT_NOT_APPROVED = new ErrorCode(1_020_016_003, "只有审批通过的合同可以开票");
    ErrorCode INVOICE_DRAFT_ONLY = new ErrorCode(1_020_016_004, "只有草稿发票可以编辑或删除");
    ErrorCode INVOICE_ISSUE_STATUS_INVALID = new ErrorCode(1_020_016_005, "只有草稿发票可以正式开具");
    ErrorCode INVOICE_AMOUNT_EXCEEDS_CONTRACT = new ErrorCode(1_020_016_006,
            "开票金额超出合同剩余可开票金额，目前可开票：{} 元");
    ErrorCode INVOICE_SPECIAL_BUYER_INFO_REQUIRED = new ErrorCode(1_020_016_007,
            "增值税专用发票必须填写税号、注册地址、注册电话、开户行和银行账号");
    ErrorCode INVOICE_VOID_STATUS_INVALID = new ErrorCode(1_020_016_008, "当前发票状态不允许作废");
    ErrorCode INVOICE_VOID_HAS_RED = new ErrorCode(1_020_016_009, "已有有效红票的蓝票不能直接作废");
    ErrorCode INVOICE_RED_ORIGINAL_INVALID = new ErrorCode(1_020_016_010, "只有有效蓝票可以红冲");
    ErrorCode INVOICE_RED_AMOUNT_EXCEEDS = new ErrorCode(1_020_016_011,
            "红冲金额超出原发票剩余可红冲金额，目前可红冲：{} 元");
    ErrorCode INVOICE_RED_CANNOT_RED = new ErrorCode(1_020_016_012, "红票不能再次红冲");
    ErrorCode INVOICE_CONCURRENT_CHANGE = new ErrorCode(1_020_016_013, "发票状态已变化，请刷新后重试");
    ErrorCode INVOICE_PROVIDER_RESULT_INVALID = new ErrorCode(1_020_016_014,
            "开票适配器返回的幂等标识无效，操作已回滚");

    // ========== 回款退款/冲销 1_020_017_000 ==========
    ErrorCode RECEIVABLE_REFUND_NOT_EXISTS = new ErrorCode(1_020_017_000, "退款/冲销记录不存在");
    ErrorCode RECEIVABLE_REFUND_NO_EXISTS = new ErrorCode(1_020_017_001, "退款/冲销编号已存在，请重试");
    ErrorCode RECEIVABLE_REFUND_SOURCE_NOT_APPROVED = new ErrorCode(1_020_017_002,
            "只有审批通过的回款可以创建退款/冲销");
    ErrorCode RECEIVABLE_REFUND_AMOUNT_EXCEEDS = new ErrorCode(1_020_017_003,
            "退款/冲销金额超出原回款剩余可退金额，目前可退：{} 元");
    ErrorCode RECEIVABLE_REFUND_EDIT_STATUS_INVALID = new ErrorCode(1_020_017_004,
            "当前退款/冲销状态不允许编辑");
    ErrorCode RECEIVABLE_REFUND_DELETE_STATUS_INVALID = new ErrorCode(1_020_017_005,
            "只有从未提交的新草稿可以删除");
    ErrorCode RECEIVABLE_REFUND_SUBMIT_STATUS_INVALID = new ErrorCode(1_020_017_006,
            "只有草稿退款/冲销可以提交审批");
    ErrorCode RECEIVABLE_REFUND_CONCURRENT_CHANGE = new ErrorCode(1_020_017_007,
            "退款/冲销状态已变化，请刷新后重试");

}
