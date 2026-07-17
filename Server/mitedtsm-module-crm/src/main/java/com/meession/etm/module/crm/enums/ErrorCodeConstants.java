package com.meession.etm.module.crm.enums;

import com.meession.etm.framework.common.exception.ErrorCode;

/**
 * CRM 错误码枚举类
 * <p>
 * crm 系统，使用 1-020-000-000 段
 */
public interface ErrorCodeConstants {

    // ========== 营销互动 1_020_014_000 ==========
    ErrorCode MARKETING_CAMPAIGN_NOT_EXISTS = new ErrorCode(1_020_014_000, "营销活动不存在");
    ErrorCode MARKETING_CAMPAIGN_CODE_EXISTS = new ErrorCode(1_020_014_001, "营销活动编码已存在");
    ErrorCode MARKETING_CAMPAIGN_TIME_INVALID = new ErrorCode(1_020_014_002, "营销活动结束时间必须晚于开始时间");
    ErrorCode MARKETING_CAMPAIGN_STATUS_INVALID = new ErrorCode(1_020_014_003, "营销活动当前状态不允许此操作");
    ErrorCode MARKETING_CAMPAIGN_LOCKED = new ErrorCode(1_020_014_004, "营销活动已锁定，不能新增关联对象");
    ErrorCode MARKETING_RELATION_NOT_EXISTS = new ErrorCode(1_020_014_005, "营销活动关联对象不存在");
    ErrorCode MARKETING_PERMISSION_DENIED = new ErrorCode(1_020_014_006, "没有营销对象操作权限");
    ErrorCode COMPETITOR_NOT_EXISTS = new ErrorCode(1_020_014_007, "竞争对手资料不存在");
    ErrorCode MARKETING_CHANNEL_INVALID = new ErrorCode(1_020_014_008, "营销渠道无效");
    ErrorCode MARKETING_TEMPLATE_REQUIRED = new ErrorCode(1_020_014_009, "所选渠道必须配置对应模板");
    ErrorCode MARKETING_BROADCAST_NOT_EXISTS = new ErrorCode(1_020_014_010, "群发任务不存在");
    ErrorCode MARKETING_BROADCAST_STATUS_INVALID = new ErrorCode(1_020_014_011, "群发任务当前状态不允许此操作");
    ErrorCode MARKETING_REVIEWER_INVALID = new ErrorCode(1_020_014_012, "群发审核人不能是创建者");
    ErrorCode MARKETING_QUOTA_EXCEEDED = new ErrorCode(1_020_014_013, "营销群发超过配置的频控或月度配额");
    ErrorCode MARKETING_RECIPIENT_NOT_FOUND = new ErrorCode(1_020_014_014, "没有可用的 CRM 联系人或客户名单");
    ErrorCode MARKETING_RECIPIENT_NONE_SENDABLE = new ErrorCode(1_020_014_021,
            "当前群发没有可发送的收件人，请查看收件人结果并补充手机号/邮箱或渠道授权");
    ErrorCode MARKETING_CONSENT_NOT_EXISTS = new ErrorCode(1_020_014_015, "营销同意记录不存在");
    ErrorCode MARKETING_CARE_PLAN_NOT_EXISTS = new ErrorCode(1_020_014_016, "客户关怀计划不存在");
    ErrorCode MARKETING_CARE_PLAN_CODE_EXISTS = new ErrorCode(1_020_014_017, "客户关怀计划编码已存在");
    ErrorCode MARKETING_REVIEW_COMMENT_REQUIRED = new ErrorCode(1_020_014_018, "驳回群发审核必须填写原因");
    ErrorCode MARKETING_CARE_RULE_INVALID = new ErrorCode(1_020_014_019, "客户关怀规则配置无效");
    ErrorCode MARKETING_CARE_PLAN_ENABLED = new ErrorCode(1_020_014_020, "启用中的客户关怀计划不能删除");

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
    ErrorCode CONTRACT_SOURCE_QUOTE_CHANGED = new ErrorCode(1_020_000_025,
            "合同来源报价已变化，不能用其他报价版本改写合同");
    ErrorCode CONTRACT_AMENDMENT_NOT_EXISTS = new ErrorCode(1_020_000_026, "合同补充协议不存在");
    ErrorCode CONTRACT_AMENDMENT_REQUIRES_APPROVED = new ErrorCode(1_020_000_027,
            "只有审批通过的合同可以创建补充协议");
    ErrorCode CONTRACT_AMENDMENT_REQUIRES_SIGNED = new ErrorCode(1_020_000_028,
            "只有已签署且未作废的合同可以创建补充协议");
    ErrorCode CONTRACT_AMENDMENT_OPEN_EXISTS = new ErrorCode(1_020_000_029,
            "当前合同已有未生效的补充协议，请完成后再创建");
    ErrorCode CONTRACT_AMENDMENT_REQUEST_CONFLICT = new ErrorCode(1_020_000_030,
            "补充协议请求号已被不同内容使用");
    ErrorCode CONTRACT_AMENDMENT_NOT_EDITABLE = new ErrorCode(1_020_000_031,
            "当前补充协议状态不允许修改");
    ErrorCode CONTRACT_AMENDMENT_NOT_DRAFT = new ErrorCode(1_020_000_032,
            "只有草稿补充协议可以提交审批");
    ErrorCode CONTRACT_AMENDMENT_BASE_VERSION_STALE = new ErrorCode(1_020_000_033,
            "合同版本已经变化，请基于最新版本重新创建补充协议");
    ErrorCode CONTRACT_AMENDMENT_EVIDENCE_REQUIRED = new ErrorCode(1_020_000_034,
            "补充协议提交审批前必须上传至少一份变更依据");
    ErrorCode CONTRACT_AMENDMENT_FINANCIAL_FLOOR = new ErrorCode(1_020_000_035,
            "补充协议金额不能低于已生效回款、回款计划或净开票金额");
    ErrorCode CONTRACT_AMENDMENT_TIME_RANGE_INVALID = new ErrorCode(1_020_000_036,
            "补充协议合同开始时间不能晚于结束时间");
    ErrorCode CONTRACT_AMENDMENT_AMOUNT_INVALID = new ErrorCode(1_020_000_037,
            "补充协议生效后的合同金额必须大于零");
    ErrorCode CONTRACT_AMENDMENT_NOT_BELONGS = new ErrorCode(1_020_000_038,
            "补充协议不属于当前合同");
    ErrorCode CONTRACT_AMENDMENT_ATTACHMENT_INVALID = new ErrorCode(1_020_000_039,
            "补充协议依据附件与补充协议状态或合同不匹配");

    // ========== 线索管理 1-020-001-000 ==========
    ErrorCode CLUE_NOT_EXISTS = new ErrorCode(1_020_001_000, "线索不存在");
    ErrorCode CLUE_TRANSFORM_FAIL_ALREADY = new ErrorCode(1_020_001_001, "线索已经转化过了，请勿重复转化");
    ErrorCode CLUE_UPDATE_FAIL_TRANSFORMED = new ErrorCode(1_020_001_002, "线索已转化为客户，只能查看");
    ErrorCode CLUE_PUBLIC_STATE_CHANGED = new ErrorCode(1_020_001_003, "公共线索【{}】状态已变化，请刷新后重试");
    ErrorCode CLUE_PUBLIC_CLAIM_REQUIRED = new ErrorCode(1_020_001_004, "公共线索必须先领取或由主管分配后才能操作");
    ErrorCode CLUE_PUBLIC_DAILY_CLAIM_LIMIT = new ErrorCode(1_020_001_005, "今日公共线索领取已达上限（{} 个/天）");
    ErrorCode CLUE_PUBLIC_REPEAT_CLAIM_COOLDOWN = new ErrorCode(1_020_001_006,
            "{} 天内不能重复领取公共线索【{}】");
    ErrorCode CLUE_OWNER_LIMIT_EXCEEDED = new ErrorCode(1_020_001_007, "负责人持有的未转换线索不能超过 {} 个");
    ErrorCode CLUE_PUBLIC_BATCH_LIMIT = new ErrorCode(1_020_001_008, "单次领取或分配公共线索不能超过 {} 个");
    ErrorCode CLUE_PUBLIC_OWNER_REQUIRED = new ErrorCode(1_020_001_009, "在管线索必须存在负责人");

    // ========== 商机管理 1-020-002-000 ==========
    ErrorCode BUSINESS_NOT_EXISTS = new ErrorCode(1_020_002_000, "商机不存在");
    ErrorCode BUSINESS_DELETE_FAIL_CONTRACT_EXISTS = new ErrorCode(1_020_002_001, "商机已关联合同，不能删除");
    ErrorCode BUSINESS_UPDATE_STATUS_FAIL_END_STATUS = new ErrorCode(1_020_002_002, "更新商机状态失败，原因：已经是结束状态");
    ErrorCode BUSINESS_UPDATE_STATUS_FAIL_STATUS_EQUALS = new ErrorCode(1_020_002_003, "更新商机状态失败，原因：已经是该状态");
    ErrorCode BUSINESS_UPDATE_STATUS_CONCURRENT = new ErrorCode(1_020_002_004, "商机状态已被其他操作修改，请刷新后重试");
    ErrorCode BUSINESS_UPDATE_STATUS_BACKWARD = new ErrorCode(1_020_002_005, "商机阶段只能向前推进，不能回退");
    ErrorCode BUSINESS_UPDATE_FAIL_END_STATUS = new ErrorCode(1_020_002_006, "已结束商机不能继续修改");

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
    ErrorCode RECEIVABLE_WRITE_OFF_NOT_EXISTS = new ErrorCode(1_020_004_010, "回款核销记录不存在");
    ErrorCode RECEIVABLE_WRITE_OFF_REQUIRES_APPROVED = new ErrorCode(1_020_004_011, "只有审批通过的回款可以核销");
    ErrorCode RECEIVABLE_WRITE_OFF_AMOUNT_EXCEEDS_REMAINING = new ErrorCode(1_020_004_012, "核销金额超过回款剩余未核销金额");
    ErrorCode RECEIVABLE_WRITE_OFF_REFERENCE_EXISTS = new ErrorCode(1_020_004_013, "外部流水号已用于其他核销记录");
    ErrorCode RECEIVABLE_WRITE_OFF_ALREADY_REVERSED = new ErrorCode(1_020_004_014, "核销记录已经冲销");

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
    ErrorCode CUSTOMER_POOL_ACTIVE_BUSINESS = new ErrorCode(1_020_006_022,
            "客户【{}】存在进行中的商机，不能移入公海");
    ErrorCode CUSTOMER_POOL_ACTIVE_CONTRACT = new ErrorCode(1_020_006_023,
            "客户【{}】存在未完结销售单据，不能移入公海");
    ErrorCode CUSTOMER_POOL_DAILY_CLAIM_LIMIT = new ErrorCode(1_020_006_024,
            "今日领取已达上限（{} 个/天），请明日再试");
    ErrorCode CUSTOMER_POOL_REPEAT_CLAIM_COOLDOWN = new ErrorCode(1_020_006_025,
            "{} 天内不可重复领取同一客户【{}】");
    ErrorCode CUSTOMER_POOL_STATE_CHANGED = new ErrorCode(1_020_006_026,
            "客户【{}】池状态已变化，请刷新后重试");
    ErrorCode CUSTOMER_POOL_MANAGE_DENIED = new ErrorCode(1_020_006_027,
            "只能将本人客户或授权范围内的下属客户移入公海");
    ErrorCode CUSTOMER_POOL_BATCH_SIZE_EXCEEDS_POLICY = new ErrorCode(1_020_006_028,
            "自动回收批量超过部署安全上限（最多 {} 条）");
    ErrorCode CUSTOMER_GARBAGE_ADMIN_REQUIRED = new ErrorCode(1_020_006_029,
            "客户垃圾池仅限 CRM 管理员操作");
    ErrorCode CUSTOMER_GARBAGE_SOURCE_INVALID = new ErrorCode(1_020_006_030,
            "只有公海客户可以转入垃圾池");
    ErrorCode CUSTOMER_GARBAGE_STATE_INVALID = new ErrorCode(1_020_006_031,
            "客户不在垃圾池或状态已变化，请刷新后重试");
    ErrorCode CUSTOMER_GARBAGE_DELETE_REFERENCED = new ErrorCode(1_020_006_032,
            "永久删除失败，客户仍有关联{}，请先解除关联");
    ErrorCode CUSTOMER_GARBAGE_ACTIVE_BUSINESS = new ErrorCode(1_020_006_033,
            "客户【{}】存在进行中的商机，不能转入垃圾池");
    ErrorCode CUSTOMER_GARBAGE_ACTIVE_CONTRACT = new ErrorCode(1_020_006_034,
            "客户【{}】存在未完结销售单据，不能转入垃圾池");

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
    ErrorCode CRM_STATISTICS_SCOPE_DENIED = new ErrorCode(1_020_014_006,
            "无权查询所选部门或人员的 CRM 统计数据");

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
    ErrorCode WORK_ORDER_GROUP_NOT_EXISTS = new ErrorCode(1_020_015_011, "客服处理组不存在");
    ErrorCode WORK_ORDER_GROUP_CODE_EXISTS = new ErrorCode(1_020_015_012, "客服处理组编码已存在");
    ErrorCode WORK_ORDER_GROUP_DISABLED = new ErrorCode(1_020_015_013, "客服处理组已停用");
    ErrorCode WORK_ORDER_GROUP_TYPE_UNSUPPORTED = new ErrorCode(1_020_015_014, "客服处理组不支持当前工单类型");
    ErrorCode WORK_ORDER_GROUP_MEMBER_REQUIRED = new ErrorCode(1_020_015_015, "客服处理组至少需要一名成员");
    ErrorCode WORK_ORDER_GROUP_MANAGER_NOT_MEMBER = new ErrorCode(1_020_015_016, "处理组负责人必须同时是组成员");
    ErrorCode WORK_ORDER_GROUP_IN_USE = new ErrorCode(1_020_015_017, "处理组仍有关联工单，不能删除");
    ErrorCode WORK_ORDER_HANDLER_NOT_ELIGIBLE = new ErrorCode(1_020_015_018, "所选处理人不在允许的派单范围内");
    ErrorCode WORK_ORDER_MANUAL_ASSIGN_DENIED = new ErrorCode(1_020_015_019, "当前用户无权手工指定处理人");
    ErrorCode WORK_ORDER_UNASSIGNED = new ErrorCode(1_020_015_020, "工单尚未分配处理人");
    ErrorCode WORK_ORDER_CLAIM_DENIED = new ErrorCode(1_020_015_021, "只有对应处理组成员可以领取未分配工单");
    ErrorCode WORK_ORDER_CC_LIMIT_EXCEEDED = new ErrorCode(1_020_015_022, "工单抄送人数超过 YAML 配置上限：{} 人");
    ErrorCode WORK_ORDER_DESCRIPTION_TOO_SHORT = new ErrorCode(1_020_015_023, "工单描述至少需要 {} 个字符");
    ErrorCode WORK_ORDER_SOLUTION_TOO_SHORT = new ErrorCode(1_020_015_024, "解决方案至少需要 {} 个字符");
    ErrorCode WORK_ORDER_HANDLER_REQUIRED = new ErrorCode(1_020_015_025, "当前 YAML 策略要求必须分配处理人");
    ErrorCode WORK_ORDER_GEOFENCE_NOT_CONFIGURED = new ErrorCode(1_020_015_026, "工单未配置服务地点，无法移动签到");
    ErrorCode WORK_ORDER_LOCATION_INVALID = new ErrorCode(1_020_015_027, "签到坐标或定位精度无效");
    ErrorCode WORK_ORDER_OUTSIDE_GEOFENCE = new ErrorCode(1_020_015_028, "当前定位不在工单服务地理围栏内");
    ErrorCode WORK_ORDER_CHECK_IN_REQUIRED = new ErrorCode(1_020_015_029, "该工单完结前必须先完成移动签到");
    ErrorCode WORK_ORDER_SLA_NOT_EXISTS = new ErrorCode(1_020_015_030, "工单 SLA 实例不存在");
    ErrorCode WORK_ORDER_SLA_PAUSE_INVALID = new ErrorCode(1_020_015_031, "当前工单 SLA 不允许暂停或已经暂停");
    ErrorCode WORK_ORDER_SLA_RESUME_INVALID = new ErrorCode(1_020_015_032, "当前工单 SLA 未暂停，无法恢复");
    ErrorCode WORK_ORDER_SLA_POLICY_INVALID = new ErrorCode(1_020_015_033, "工单 SLA 策略无效");

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

    // ========== 报销与费用分类 1_020_018_000 ==========
    ErrorCode REIMBURSEMENT_NOT_EXISTS = new ErrorCode(1_020_018_000, "报销单不存在");
    ErrorCode REIMBURSEMENT_NO_EXISTS = new ErrorCode(1_020_018_001, "报销编号已存在，请重试");
    ErrorCode REIMBURSEMENT_ITEM_REQUIRED = new ErrorCode(1_020_018_002, "报销单至少需要一条费用明细");
    ErrorCode REIMBURSEMENT_ITEM_AMOUNT_INVALID = new ErrorCode(1_020_018_003, "费用明细金额必须大于 0");
    ErrorCode REIMBURSEMENT_CATEGORY_NOT_EXISTS = new ErrorCode(1_020_018_004, "费用分类不存在");
    ErrorCode REIMBURSEMENT_CATEGORY_DISABLED = new ErrorCode(1_020_018_005, "费用分类【{}】已停用");
    ErrorCode REIMBURSEMENT_CATEGORY_CODE_EXISTS = new ErrorCode(1_020_018_006, "费用分类编码已存在");
    ErrorCode REIMBURSEMENT_CATEGORY_NAME_EXISTS = new ErrorCode(1_020_018_007, "费用分类名称已存在");
    ErrorCode REIMBURSEMENT_CATEGORY_USED = new ErrorCode(1_020_018_008, "费用分类已被报销明细使用，不能删除");
    ErrorCode REIMBURSEMENT_CUSTOMER_NOT_EXISTS = new ErrorCode(1_020_018_009, "关联客户不存在");
    ErrorCode REIMBURSEMENT_CONTRACT_NOT_EXISTS = new ErrorCode(1_020_018_010, "关联合同不存在");
    ErrorCode REIMBURSEMENT_CONTRACT_CUSTOMER_MISMATCH = new ErrorCode(1_020_018_011, "合同与所选客户不一致");
    ErrorCode REIMBURSEMENT_DATE_RANGE_INVALID = new ErrorCode(1_020_018_012, "费用开始日期不能晚于结束日期");
    ErrorCode REIMBURSEMENT_EDIT_STATUS_INVALID = new ErrorCode(1_020_018_013, "当前报销状态不允许编辑");
    ErrorCode REIMBURSEMENT_DELETE_STATUS_INVALID = new ErrorCode(1_020_018_014, "只有从未提交的新草稿可以删除");
    ErrorCode REIMBURSEMENT_SUBMIT_STATUS_INVALID = new ErrorCode(1_020_018_015, "只有草稿报销单可以提交审批");
    ErrorCode REIMBURSEMENT_CONCURRENT_CHANGE = new ErrorCode(1_020_018_016, "报销状态已变化，请刷新后重试");
    ErrorCode REIMBURSEMENT_ITEM_DATE_INVALID = new ErrorCode(1_020_018_017, "费用发生日期必须在报销日期范围内");
    ErrorCode REIMBURSEMENT_TOTAL_AMOUNT_INVALID = new ErrorCode(1_020_018_018, "报销总金额超出系统支持范围");
    ErrorCode REIMBURSEMENT_ATTACHMENT_NOT_MANAGED = new ErrorCode(1_020_018_019, "报销附件必须来自系统受管文件存储");
    ErrorCode REIMBURSEMENT_ATTACHMENT_NOT_PROTECTED = new ErrorCode(1_020_018_020, "报销附件不属于当前报销单的受保护目录");

    // ========== CRM 任务、通话、短信活动 1_020_019_000 ==========
    ErrorCode ACTIVITY_BIZ_TYPE_INVALID = new ErrorCode(1_020_019_000, "CRM 活动只能关联线索或客户");
    ErrorCode TASK_NOT_EXISTS = new ErrorCode(1_020_019_001, "CRM 任务不存在");
    ErrorCode TASK_TYPE_INVALID = new ErrorCode(1_020_019_002, "CRM 任务类型无效");
    ErrorCode TASK_PRIORITY_INVALID = new ErrorCode(1_020_019_003, "CRM 任务优先级无效");
    ErrorCode TASK_TIME_INVALID = new ErrorCode(1_020_019_004, "任务截止时间必须晚于当前时间，提醒时间不能晚于截止时间");
    ErrorCode TASK_EDIT_DENIED = new ErrorCode(1_020_019_005, "只有创建人可以修改未开始的任务");
    ErrorCode TASK_ASSIGNEE_ONLY = new ErrorCode(1_020_019_006, "只有任务负责人可以开始或处理任务");
    ErrorCode TASK_TRANSITION_INVALID = new ErrorCode(1_020_019_007, "任务状态已变化或当前状态不允许该操作");
    ErrorCode TASK_RESULT_REQUIRED = new ErrorCode(1_020_019_008, "未完成或取消任务必须填写原因");
    ErrorCode CALL_DIRECTION_INVALID = new ErrorCode(1_020_019_009, "通话方向无效");
    ErrorCode CALL_STATUS_INVALID = new ErrorCode(1_020_019_010, "通话状态无效");
    ErrorCode CALL_TIME_INVALID = new ErrorCode(1_020_019_011, "接通通话必须有不早于开始时间的结束时间");
    ErrorCode CALL_RECORDING_PATH_INVALID = new ErrorCode(1_020_019_012, "通话录音必须来自 CRM 受保护目录");
    ErrorCode SMS_DIRECTION_INVALID = new ErrorCode(1_020_019_013, "短信方向无效");
    ErrorCode SMS_STATUS_INVALID = new ErrorCode(1_020_019_014, "短信方向和状态不一致");
    ErrorCode SMS_FAILURE_REASON_REQUIRED = new ErrorCode(1_020_019_015, "发送失败的短信必须记录失败原因");
    ErrorCode CLUE_CONVERSION_AUDIT_EXISTS = new ErrorCode(1_020_019_016, "线索转换迁移审计已存在");

    // ========== 商机报价版本 1_020_020_000 ==========
    ErrorCode QUOTE_NOT_EXISTS = new ErrorCode(1_020_020_000, "商机报价不存在");
    ErrorCode QUOTE_LOCKED_IMMUTABLE = new ErrorCode(1_020_020_001, "当前报价已锁定，请重开新版本后再修改");
    ErrorCode QUOTE_LOCK_STATUS_INVALID = new ErrorCode(1_020_020_002, "只有当前报价草稿可以锁定");
    ErrorCode QUOTE_REOPEN_STATUS_INVALID = new ErrorCode(1_020_020_003, "只有当前已锁定报价可以重开");
    ErrorCode QUOTE_REOPEN_BUSINESS_ENDED = new ErrorCode(1_020_020_004, "已结束商机不能重开报价");
    ErrorCode QUOTE_CURRENT_NOT_LOCKED = new ErrorCode(1_020_020_005, "商机必须先锁定当前报价才能赢单或转换合同");
    ErrorCode QUOTE_ITEM_REQUIRED = new ErrorCode(1_020_020_006, "锁定报价至少需要一条产品明细");
    ErrorCode QUOTE_ITEM_AMOUNT_INVALID = new ErrorCode(1_020_020_007, "报价单价不能为负且数量必须大于 0");
    ErrorCode QUOTE_DISCOUNT_INVALID = new ErrorCode(1_020_020_008, "报价整单折扣必须在 0 到 100 之间");
    ErrorCode QUOTE_CURRENCY_UNSUPPORTED = new ErrorCode(1_020_020_009, "报价币种【{}】未在 YAML 汇率表中配置");
    ErrorCode QUOTE_TAX_RATE_UNSUPPORTED = new ErrorCode(1_020_020_010, "报价税率【{}】未在 YAML 白名单中配置");
    ErrorCode QUOTE_VERSION_LIMIT = new ErrorCode(1_020_020_011, "报价版本已达到 YAML 上限 {}，不能继续重开");
    ErrorCode QUOTE_VERSION_CONCURRENT = new ErrorCode(1_020_020_012, "报价版本已被其他请求修改，请刷新后重试");
    ErrorCode QUOTE_ACTION_REMARK_REQUIRED = new ErrorCode(1_020_020_013, "锁定或重开报价必须填写原因");
    ErrorCode QUOTE_TERMINATE_STATUS_INVALID = new ErrorCode(1_020_020_014,
            "只有当前草稿或锁定报价可以随商机终止");

    // ========== CRM 到 ERP 履约 1_020_021_000 ==========
    ErrorCode ERP_FULFILLMENT_DISABLED = new ErrorCode(1_020_021_000, "CRM 到 ERP 履约功能未在 YAML 中启用");
    ErrorCode ERP_CUSTOMER_MAPPING_NOT_EXISTS = new ErrorCode(1_020_021_001,
            "CRM 客户【{}】尚未映射到 ERP 客户");
    ErrorCode ERP_PRODUCT_MAPPING_NOT_EXISTS = new ErrorCode(1_020_021_002,
            "合同产品【{}】尚未映射到 ERP 产品");
    ErrorCode ERP_CUSTOMER_MAPPING_CONFLICT = new ErrorCode(1_020_021_003,
            "ERP 客户【{}】已经映射到其他 CRM 客户");
    ErrorCode ERP_PRODUCT_MAPPING_CONFLICT = new ErrorCode(1_020_021_004,
            "ERP 产品【{}】已经映射到其他 CRM 产品");
    ErrorCode ERP_MAPPING_NOT_EXISTS = new ErrorCode(1_020_021_005, "CRM—ERP 映射不存在");
    ErrorCode CONTRACT_FULFILLMENT_REQUIRES_APPROVED = new ErrorCode(1_020_021_006,
            "只有审批通过的合同可以创建 ERP 履约订单");
    ErrorCode CONTRACT_FULFILLMENT_REQUIRES_SIGNED = new ErrorCode(1_020_021_007,
            "合同必须完成有效签署后才能创建 ERP 履约订单");
    ErrorCode CONTRACT_FULFILLMENT_PRODUCT_REQUIRED = new ErrorCode(1_020_021_008,
            "合同至少需要一条产品快照才能创建 ERP 履约订单");
    ErrorCode CONTRACT_FULFILLMENT_CURRENCY_UNSUPPORTED = new ErrorCode(1_020_021_009,
            "合同币种【{}】未在 ERP 履约 YAML 白名单中配置");
    ErrorCode CONTRACT_FULFILLMENT_CURRENCY_MISMATCH = new ErrorCode(1_020_021_010,
            "合同币种【{}】与 ERP 币种【{}】不一致，当前策略禁止换算");
    ErrorCode CONTRACT_FULFILLMENT_BASE_CURRENCY_MISMATCH = new ErrorCode(1_020_021_011,
            "合同本位币【{}】与 ERP 币种【{}】不一致，不能执行显式换算");
    ErrorCode CONTRACT_FULFILLMENT_RATE_INVALID = new ErrorCode(1_020_021_012,
            "合同兑 ERP 币种汇率无效");
    ErrorCode CONTRACT_FULFILLMENT_AMOUNT_MISMATCH = new ErrorCode(1_020_021_013,
            "转换后的 ERP 明细金额【{}】与合同本位币含税金额【{}】差异超过 YAML 容差【{}】");
    ErrorCode CONTRACT_FULFILLMENT_REQUEST_CORRUPTED = new ErrorCode(1_020_021_014,
            "履约请求快照无法读取，请检查数据完整性");
    ErrorCode CONTRACT_FULFILLMENT_CREATE_FAILED = new ErrorCode(1_020_021_015,
            "ERP 履约订单创建失败：{}");
    ErrorCode CONTRACT_FULFILLMENT_NOT_EXISTS = new ErrorCode(1_020_021_016,
            "合同尚未创建 ERP 履约记录");
    ErrorCode CONTRACT_FULFILLMENT_NOT_CREATED = new ErrorCode(1_020_021_017,
            "ERP 履约订单尚未创建成功，不能刷新状态");
    ErrorCode CONTRACT_FULFILLMENT_ERP_ORDER_MISSING = new ErrorCode(1_020_021_018,
            "已记录的 ERP 履约订单不存在，请人工核对，不会自动重建第二张订单");
    ErrorCode CONTRACT_FULFILLMENT_SNAPSHOT_CONFLICT = new ErrorCode(1_020_021_019,
            "合同已有不同的履约请求快照，已阻止重复生成 ERP 订单");

    // ========== 客户拜访 1_020_022_000 ==========
    ErrorCode CUSTOMER_VISIT_NOT_EXISTS = new ErrorCode(1_020_022_000, "客户拜访不存在或无权访问");
    ErrorCode CUSTOMER_VISIT_CONTACT_MISMATCH = new ErrorCode(1_020_022_001, "所选联系人不属于当前客户");
    ErrorCode CUSTOMER_VISIT_RESULT_STATUS_INVALID = new ErrorCode(1_020_022_002, "只有审批通过且未回填结果的拜访可以登记结果");
    ErrorCode CUSTOMER_VISIT_RESULT_TIME_INVALID = new ErrorCode(1_020_022_003, "实际结束时间不能早于实际开始时间或晚于当前时间");
    ErrorCode CUSTOMER_VISIT_RESULT_ALREADY_RECORDED = new ErrorCode(1_020_022_004, "拜访结果已登记，不能重复生成客户跟进记录");

    // ========== OA 工作报告 1_020_023_000 ==========
    ErrorCode WORK_REPORT_NOT_EXISTS = new ErrorCode(1_020_023_000, "工作报告不存在或无权访问");
    ErrorCode WORK_REPORT_TYPE_INVALID = new ErrorCode(1_020_023_001, "工作报告类型无效");
    ErrorCode WORK_REPORT_PERIOD_DUPLICATE = new ErrorCode(1_020_023_002, "当前报告类型和周期已存在报告");
    ErrorCode WORK_REPORT_SUBMITTED_IMMUTABLE = new ErrorCode(1_020_023_003, "已提交的工作报告不能修改或删除");
    ErrorCode WORK_REPORT_RECEIVER_REQUIRED = new ErrorCode(1_020_023_004, "工作报告至少需要一名接收人");

}
