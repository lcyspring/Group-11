-- ========== 工单模块 DDL ==========

-- 工单类型表
CREATE TABLE IF NOT EXISTS wo_work_order_type (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    name VARCHAR(100) NOT NULL COMMENT '类型名称',
    code VARCHAR(50) NOT NULL COMMENT '类型编码',
    description VARCHAR(255) COMMENT '类型描述',
    sort INT DEFAULT 0 COMMENT '排序',
    status TINYINT DEFAULT 0 COMMENT '状态: 0-启用, 1-禁用',
    creator VARCHAR(64) DEFAULT '' COMMENT '创建者',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updater VARCHAR(64) DEFAULT '' COMMENT '更新者',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted BIT NOT NULL DEFAULT 0 COMMENT '是否删除',
    tenant_id BIGINT DEFAULT 0 COMMENT '租户编号',
    PRIMARY KEY (id)
) COMMENT '工单类型表';

-- 工单表
CREATE TABLE IF NOT EXISTS wo_work_order (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '工单编号',
    title VARCHAR(200) NOT NULL COMMENT '工单标题',
    content TEXT COMMENT '工单内容/描述',
    type_id BIGINT COMMENT '工单类型编号',
    priority TINYINT DEFAULT 0 COMMENT '优先级: 0-低, 1-中, 2-高, 3-紧急',
    status TINYINT DEFAULT 0 COMMENT '工单状态: 0-待处理, 1-处理中, 2-已完成, 3-已关闭, 4-已退回',
    handler_user_id BIGINT COMMENT '处理人用户编号',
    submitter_user_id BIGINT COMMENT '发起人用户编号',
    result TEXT COMMENT '处理结果/备注',
    handle_time DATETIME COMMENT '处理时间',
    expected_finish_time DATETIME COMMENT '预计完成时间',
    finish_time DATETIME COMMENT '实际完成时间',
    customer_id BIGINT COMMENT '关联客户编号(可选)',
    business_id BIGINT COMMENT '关联商机编号(可选)',
    remark VARCHAR(500) COMMENT '备注',
    creator VARCHAR(64) DEFAULT '' COMMENT '创建者',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updater VARCHAR(64) DEFAULT '' COMMENT '更新者',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted BIT NOT NULL DEFAULT 0 COMMENT '是否删除',
    tenant_id BIGINT DEFAULT 0 COMMENT '租户编号',
    PRIMARY KEY (id)
) COMMENT '工单表';
