-- ============================================================
-- MITEDTSM (密讯ETM系统) - 数据库初始化脚本
-- 版本: v1.0
-- 日期: 2026-03-17
-- 执行方式: mysql -u root -p < init_db.sql
-- 注意: 本项目实际 SQL 位于 InstallPackage/database/base/
--       此文件是通用参考版本
-- ============================================================

-- ----------------------------------------------------------
-- 1. 创建数据库
-- ----------------------------------------------------------
CREATE DATABASE IF NOT EXISTS mitedtsm_database
    DEFAULT CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;

USE mitedtsm_database;

-- ----------------------------------------------------------
-- 2. 系统管理模块表 (system_*)
--    注意: 本项目实际表前缀为 system_ 而非 sys_
--    此处使用 sys_ 作为通用参考
-- ----------------------------------------------------------

-- 2.1 用户表
CREATE TABLE IF NOT EXISTS system_user (
    id          BIGINT PRIMARY KEY AUTO_INCREMENT,
    username    VARCHAR(50)  NOT NULL UNIQUE COMMENT '用户名',
    password    VARCHAR(200) NOT NULL COMMENT 'BCrypt加密密码',
    nickname    VARCHAR(50)  COMMENT '昵称',
    email       VARCHAR(100) COMMENT '邮箱',
    phone       VARCHAR(20)  COMMENT '手机号',
    avatar      VARCHAR(500) COMMENT '头像URL',
    gender      TINYINT DEFAULT 0 COMMENT '性别: 0未知 1男 2女',
    status      TINYINT DEFAULT 1 COMMENT '状态: 1正常 0禁用',
    tenant_id   BIGINT DEFAULT 0 COMMENT '租户ID, 0=平台',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted     TINYINT DEFAULT 0 COMMENT '逻辑删除: 0未删除 1已删除',
    INDEX idx_username (username),
    INDEX idx_tenant_id (tenant_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户表';

-- 2.2 角色表
CREATE TABLE IF NOT EXISTS system_role (
    id          BIGINT PRIMARY KEY AUTO_INCREMENT,
    role_code   VARCHAR(50)  NOT NULL UNIQUE COMMENT '角色编码',
    role_name   VARCHAR(50)  NOT NULL COMMENT '角色名称',
    role_level  VARCHAR(20)  DEFAULT 'TENANT' COMMENT '级别: PLATFORM/TENANT',
    data_scope  VARCHAR(20)  DEFAULT 'SELF' COMMENT '数据范围: ALL/TENANT/DEPT/SELF',
    tenant_id   BIGINT DEFAULT 0 COMMENT '租户ID, 0=平台',
    status      TINYINT DEFAULT 1 COMMENT '状态: 1正常 0禁用',
    sort_order  INT DEFAULT 0 COMMENT '排序',
    remark      VARCHAR(200) COMMENT '备注',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted     TINYINT DEFAULT 0,
    INDEX idx_role_code (role_code),
    INDEX idx_tenant_id (tenant_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='角色表';

-- 2.3 菜单表
CREATE TABLE IF NOT EXISTS system_menu (
    id          BIGINT PRIMARY KEY AUTO_INCREMENT,
    parent_id   BIGINT DEFAULT 0 COMMENT '父菜单ID',
    menu_name   VARCHAR(50)  NOT NULL COMMENT '菜单名称',
    menu_type   VARCHAR(20)  DEFAULT 'MENU' COMMENT '类型: MENU/BUTTON/DIRECTORY',
    permission  VARCHAR(100) COMMENT '权限编码',
    path        VARCHAR(200) COMMENT '路由路径',
    component   VARCHAR(200) COMMENT '组件路径',
    icon        VARCHAR(100) COMMENT '图标',
    sort_order  INT DEFAULT 0 COMMENT '排序',
    visible     TINYINT DEFAULT 1 COMMENT '是否可见',
    status      TINYINT DEFAULT 1 COMMENT '状态',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted     TINYINT DEFAULT 0,
    INDEX idx_parent_id (parent_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='菜单表';

-- 2.4 用户角色关联表
CREATE TABLE IF NOT EXISTS system_user_role (
    id      BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    role_id BIGINT NOT NULL,
    UNIQUE KEY uk_user_role (user_id, role_id),
    INDEX idx_user_id (user_id),
    INDEX idx_role_id (role_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户角色关联表';

-- 2.5 角色菜单关联表
CREATE TABLE IF NOT EXISTS system_role_menu (
    id      BIGINT PRIMARY KEY AUTO_INCREMENT,
    role_id BIGINT NOT NULL,
    menu_id BIGINT NOT NULL,
    UNIQUE KEY uk_role_menu (role_id, menu_id),
    INDEX idx_role_id (role_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='角色菜单关联表';

-- 2.6 部门表
CREATE TABLE IF NOT EXISTS system_dept (
    id          BIGINT PRIMARY KEY AUTO_INCREMENT,
    parent_id   BIGINT DEFAULT 0 COMMENT '父部门ID',
    dept_name   VARCHAR(50) NOT NULL COMMENT '部门名称',
    dept_code   VARCHAR(50) COMMENT '部门编码',
    leader      VARCHAR(50) COMMENT '负责人',
    phone       VARCHAR(20) COMMENT '联系电话',
    sort_order  INT DEFAULT 0 COMMENT '排序',
    tenant_id   BIGINT DEFAULT 0 COMMENT '租户ID',
    status      TINYINT DEFAULT 1 COMMENT '状态',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted     TINYINT DEFAULT 0,
    INDEX idx_parent_id (parent_id),
    INDEX idx_tenant_id (tenant_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='部门表';

-- 2.7 字典类型表
CREATE TABLE IF NOT EXISTS system_dict_type (
    id          BIGINT PRIMARY KEY AUTO_INCREMENT,
    dict_code   VARCHAR(50) NOT NULL UNIQUE COMMENT '字典编码',
    dict_name   VARCHAR(50) NOT NULL COMMENT '字典名称',
    status      TINYINT DEFAULT 1 COMMENT '状态',
    remark      VARCHAR(200) COMMENT '备注',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='字典类型表';

-- 2.8 字典数据表
CREATE TABLE IF NOT EXISTS system_dict_data (
    id          BIGINT PRIMARY KEY AUTO_INCREMENT,
    dict_code   VARCHAR(50) NOT NULL COMMENT '字典编码',
    dict_label  VARCHAR(100) NOT NULL COMMENT '字典标签',
    dict_value  VARCHAR(100) NOT NULL COMMENT '字典值',
    sort_order  INT DEFAULT 0 COMMENT '排序',
    css_class   VARCHAR(50) COMMENT '样式类名',
    status      TINYINT DEFAULT 1 COMMENT '状态',
    remark      VARCHAR(200) COMMENT '备注',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_dict_code (dict_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='字典数据表';

-- 2.9 操作日志表
CREATE TABLE IF NOT EXISTS system_oper_log (
    id          BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id     BIGINT COMMENT '用户ID',
    username    VARCHAR(50) COMMENT '用户名',
    module      VARCHAR(50) COMMENT '模块名',
    action      VARCHAR(50) COMMENT '操作类型',
    method      VARCHAR(200) COMMENT '请求方法',
    request_url VARCHAR(500) COMMENT '请求URL',
    request_params TEXT COMMENT '请求参数',
    ip          VARCHAR(50) COMMENT 'IP地址',
    status      TINYINT DEFAULT 1 COMMENT '操作结果 1成功 0失败',
    error_msg   TEXT COMMENT '错误信息',
    cost_time   BIGINT COMMENT '耗时(ms)',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_user_id (user_id),
    INDEX idx_create_time (create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='操作日志表';

-- 2.10 租户表
CREATE TABLE IF NOT EXISTS system_tenant (
    id          BIGINT PRIMARY KEY AUTO_INCREMENT,
    tenant_name VARCHAR(50)  NOT NULL COMMENT '租户名称',
    tenant_code VARCHAR(50)  NOT NULL UNIQUE COMMENT '租户编码',
    contact_person VARCHAR(50) COMMENT '联系人',
    contact_phone VARCHAR(20) COMMENT '联系电话',
    status      TINYINT DEFAULT 1 COMMENT '状态: 1正常 0禁用',
    expire_time DATETIME COMMENT '到期时间',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted     TINYINT DEFAULT 0
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='租户表';

-- ----------------------------------------------------------
-- 3. 人力资源模块表 (hrm_*)
-- ----------------------------------------------------------

-- 3.1 员工表
CREATE TABLE IF NOT EXISTS hrm_employee (
    id              BIGINT PRIMARY KEY AUTO_INCREMENT,
    emp_no          VARCHAR(50) NOT NULL UNIQUE COMMENT '工号',
    emp_name        VARCHAR(50) NOT NULL COMMENT '姓名',
    user_id         BIGINT COMMENT '关联系统用户ID',
    dept_id         BIGINT COMMENT '所属部门ID',
    gender          TINYINT DEFAULT 0 COMMENT '性别: 0未知 1男 2女',
    id_card         VARCHAR(20) COMMENT '身份证号',
    birthday        DATE COMMENT '出生日期',
    phone           VARCHAR(20) COMMENT '手机号',
    email           VARCHAR(100) COMMENT '邮箱',
    education       VARCHAR(20) COMMENT '学历',
    entry_date      DATE COMMENT '入职日期',
    leave_date      DATE COMMENT '离职日期',
    emp_status      VARCHAR(20) DEFAULT 'IN_SERVICE' COMMENT '员工状态',
    position        VARCHAR(50) COMMENT '职位',
    tenant_id       BIGINT DEFAULT 0 COMMENT '租户ID',
    create_time     DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time     DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted         TINYINT DEFAULT 0,
    INDEX idx_emp_no (emp_no),
    INDEX idx_dept_id (dept_id),
    INDEX idx_tenant_id (tenant_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='员工表';

-- 3.2 考勤记录表
CREATE TABLE IF NOT EXISTS hrm_attendance (
    id          BIGINT PRIMARY KEY AUTO_INCREMENT,
    emp_id      BIGINT NOT NULL COMMENT '员工ID',
    att_date    DATE NOT NULL COMMENT '考勤日期',
    checkin_time  DATETIME COMMENT '签到时间',
    checkout_time DATETIME COMMENT '签退时间',
    status      VARCHAR(20) DEFAULT 'NORMAL' COMMENT '考勤状态: NORMAL/LATE/EARLY/ABSENT',
    remark      VARCHAR(200) COMMENT '备注',
    tenant_id   BIGINT DEFAULT 0 COMMENT '租户ID',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_emp_id (emp_id),
    INDEX idx_att_date (att_date),
    INDEX idx_tenant_id (tenant_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='考勤记录表';

-- 3.3 薪资记录表
CREATE TABLE IF NOT EXISTS hrm_salary (
    id              BIGINT PRIMARY KEY AUTO_INCREMENT,
    emp_id          BIGINT NOT NULL COMMENT '员工ID',
    salary_month    VARCHAR(7) NOT NULL COMMENT '薪资月份 YYYY-MM',
    basic_salary    DECIMAL(12,2) DEFAULT 0.00 COMMENT '基本工资',
    bonus           DECIMAL(12,2) DEFAULT 0.00 COMMENT '奖金',
    deduction       DECIMAL(12,2) DEFAULT 0.00 COMMENT '扣款',
    total_salary    DECIMAL(12,2) DEFAULT 0.00 COMMENT '实发工资',
    tenant_id       BIGINT DEFAULT 0 COMMENT '租户ID',
    create_time     DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time     DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_emp_id (emp_id),
    INDEX idx_salary_month (salary_month),
    INDEX idx_tenant_id (tenant_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='薪资记录表';

-- ----------------------------------------------------------
-- 4. 办公自动化模块表 (oa_*)
-- ----------------------------------------------------------

-- 4.1 通知公告表
CREATE TABLE IF NOT EXISTS oa_notice (
    id          BIGINT PRIMARY KEY AUTO_INCREMENT,
    title       VARCHAR(200) NOT NULL COMMENT '标题',
    content     LONGTEXT COMMENT '内容',
    notice_type VARCHAR(20) DEFAULT 'NOTICE' COMMENT '类型: NOTICE/ANNOUNCEMENT/MESSAGE',
    sender_id   BIGINT COMMENT '发送人ID',
    level       VARCHAR(20) DEFAULT 'NORMAL' COMMENT '级别: NORMAL/IMPORTANT/URGENT',
    status      VARCHAR(20) DEFAULT 'PUBLISHED' COMMENT '状态: DRAFT/PUBLISHED/REVOKED',
    publish_time DATETIME COMMENT '发布时间',
    tenant_id   BIGINT DEFAULT 0 COMMENT '租户ID',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted     TINYINT DEFAULT 0,
    INDEX idx_notice_type (notice_type),
    INDEX idx_tenant_id (tenant_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='通知公告表';

-- 4.2 通知阅读记录表
CREATE TABLE IF NOT EXISTS oa_notice_read (
    id          BIGINT PRIMARY KEY AUTO_INCREMENT,
    notice_id   BIGINT NOT NULL COMMENT '通知ID',
    user_id     BIGINT NOT NULL COMMENT '用户ID',
    read_time   DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '阅读时间',
    UNIQUE KEY uk_notice_user (notice_id, user_id),
    INDEX idx_notice_id (notice_id),
    INDEX idx_user_id (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='通知阅读记录表';

-- 4.3 流程定义表
CREATE TABLE IF NOT EXISTS oa_process_definition (
    id              BIGINT PRIMARY KEY AUTO_INCREMENT,
    process_key     VARCHAR(100) NOT NULL UNIQUE COMMENT '流程Key',
    process_name    VARCHAR(100) NOT NULL COMMENT '流程名称',
    process_desc    VARCHAR(500) COMMENT '流程描述',
    category        VARCHAR(50) COMMENT '分类',
    version         INT DEFAULT 1 COMMENT '版本',
    status          VARCHAR(20) DEFAULT 'ACTIVE' COMMENT '状态: ACTIVE/SUSPENDED',
    form_config     JSON COMMENT '表单配置JSON',
    tenant_id       BIGINT DEFAULT 0 COMMENT '租户ID',
    create_time     DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time     DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted         TINYINT DEFAULT 0,
    INDEX idx_process_key (process_key),
    INDEX idx_tenant_id (tenant_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='流程定义表';

-- 4.4 流程实例表
CREATE TABLE IF NOT EXISTS oa_process_instance (
    id              BIGINT PRIMARY KEY AUTO_INCREMENT,
    definition_id   BIGINT NOT NULL COMMENT '流程定义ID',
    process_key     VARCHAR(100) COMMENT '流程Key',
    business_key    VARCHAR(100) COMMENT '业务Key',
    title           VARCHAR(200) COMMENT '流程标题',
    initiator_id    BIGINT NOT NULL COMMENT '发起人ID',
    current_node    VARCHAR(100) COMMENT '当前节点',
    status          VARCHAR(20) DEFAULT 'RUNNING' COMMENT '状态: RUNNING/COMPLETED/TERMINATED',
    form_data       JSON COMMENT '表单数据JSON',
    start_time      DATETIME COMMENT '开始时间',
    end_time        DATETIME COMMENT '结束时间',
    tenant_id       BIGINT DEFAULT 0 COMMENT '租户ID',
    create_time     DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time     DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_initiator_id (initiator_id),
    INDEX idx_status (status),
    INDEX idx_tenant_id (tenant_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='流程实例表';

-- 4.5 审批记录表
CREATE TABLE IF NOT EXISTS oa_approval_record (
    id              BIGINT PRIMARY KEY AUTO_INCREMENT,
    instance_id     BIGINT NOT NULL COMMENT '流程实例ID',
    node_name       VARCHAR(100) COMMENT '节点名称',
    approver_id     BIGINT NOT NULL COMMENT '审批人ID',
    action          VARCHAR(20) NOT NULL COMMENT '操作: APPROVE/REJECT/DELEGATE',
    comment         TEXT COMMENT '审批意见',
    approve_time    DATETIME COMMENT '审批时间',
    create_time     DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_instance_id (instance_id),
    INDEX idx_approver_id (approver_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='审批记录表';

-- 4.6 日程管理表
CREATE TABLE IF NOT EXISTS oa_schedule (
    id          BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id     BIGINT NOT NULL COMMENT '用户ID',
    title       VARCHAR(200) NOT NULL COMMENT '日程标题',
    content     TEXT COMMENT '日程内容',
    start_time  DATETIME NOT NULL COMMENT '开始时间',
    end_time    DATETIME NOT NULL COMMENT '结束时间',
    location    VARCHAR(200) COMMENT '地点',
    color       VARCHAR(20) DEFAULT '#1890ff' COMMENT '颜色标记',
    remind      TINYINT DEFAULT 0 COMMENT '是否提醒',
    remind_time INT DEFAULT 30 COMMENT '提前提醒分钟数',
    tenant_id   BIGINT DEFAULT 0 COMMENT '租户ID',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted     TINYINT DEFAULT 0,
    INDEX idx_user_id (user_id),
    INDEX idx_tenant_id (tenant_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='日程管理表';

-- ----------------------------------------------------------
-- 5. 插入初始数据
-- ----------------------------------------------------------

-- 5.1 租户数据
INSERT INTO system_tenant (id, tenant_name, tenant_code, contact_person, contact_phone) VALUES
(0, '平台管理', 'PLATFORM', '超级管理员', '13800000000'),
(1, '密讯科技', 'DEFAULT_TENANT', '租户管理员', '13800000001');

-- 5.2 字典类型
INSERT INTO system_dict_type (id, dict_code, dict_name, remark) VALUES
(1, 'sys_user_status', '用户状态', '系统用户状态'),
(2, 'sys_gender', '性别', '性别字典'),
(3, 'hrm_edu_level', '学历', '学历字典'),
(4, 'hrm_emp_status', '员工状态', '员工状态'),
(5, 'oa_process_status', '流程状态', '流程状态'),
(6, 'oa_notice_type', '通知类型', '通知公告类型');

-- 5.3 字典数据
INSERT INTO system_dict_data (dict_code, dict_label, dict_value, sort_order) VALUES
('sys_user_status', '正常', '1', 1),
('sys_user_status', '禁用', '0', 2),
('sys_gender', '未知', '0', 1),
('sys_gender', '男', '1', 2),
('sys_gender', '女', '2', 3),
('hrm_edu_level', '专科', 'ASSOCIATE', 1),
('hrm_edu_level', '本科', 'BACHELOR', 2),
('hrm_edu_level', '硕士', 'MASTER', 3),
('hrm_edu_level', '博士', 'DOCTOR', 4),
('hrm_emp_status', '在职', 'IN_SERVICE', 1),
('hrm_emp_status', '离职', 'LEFT', 2),
('hrm_emp_status', '实习', 'INTERN', 3),
('hrm_emp_status', '试用', 'PROBATION', 4),
('oa_process_status', '草稿', 'DRAFT', 1),
('oa_process_status', '审批中', 'RUNNING', 2),
('oa_process_status', '已通过', 'COMPLETED', 3),
('oa_process_status', '已驳回', 'REJECTED', 4),
('oa_notice_type', '系统通知', 'NOTICE', 1),
('oa_notice_type', '公告', 'ANNOUNCEMENT', 2),
('oa_notice_type', '消息', 'MESSAGE', 3);

-- 5.4 角色数据
INSERT INTO system_role (id, role_code, role_name, role_level, data_scope, tenant_id, sort_order, remark) VALUES
(1, 'ROLE_SUPER_ADMIN',  '平台管理员', 'PLATFORM', 'ALL',    0, 1, '系统最高权限，管理所有租户'),
(2, 'ROLE_TENANT_ADMIN', '租户管理员', 'TENANT',   'TENANT', 1, 2, '管理本租户所有数据'),
(3, 'ROLE_TEACHER',      '教师',       'TENANT',   'DEPT',   1, 3, '教师角色，管理本部门数据'),
(4, 'ROLE_STUDENT',      '学生',       'TENANT',   'SELF',   1, 4, '学生角色，仅管理本人数据'),
(5, 'ROLE_TESTER',       '测试用户',   'TENANT',   'SELF',   1, 5, '测试专用');

-- 5.5 菜单数据
INSERT INTO system_menu (id, parent_id, menu_name, menu_type, permission, path, component, icon, sort_order) VALUES
-- 根目录
(1,  0, '首页',     'DIRECTORY', NULL,                  '/dashboard',  'dashboard/index',  'HomeOutlined',      1),
(2,  0, '系统管理', 'DIRECTORY', NULL,                  '/system',     'Layout',           'SettingOutlined',   2),
(3,  0, '人力资源', 'DIRECTORY', NULL,                  '/hrm',        'Layout',           'TeamOutlined',      3),
(4,  0, '办公自动化','DIRECTORY', NULL,                 '/oa',         'Layout',           'FileTextOutlined',  4),
(5,  0, '个人中心', 'DIRECTORY', NULL,                  '/profile',    'Layout',           'UserOutlined',      5),
-- 系统管理子菜单
(11, 2, '用户管理', 'MENU', 'system:user:list',      '/system/user',   'system/user/index',   'UserOutlined',      1),
(12, 2, '角色管理', 'MENU', 'system:role:list',      '/system/role',   'system/role/index',   'SafetyOutlined',    2),
(13, 2, '菜单管理', 'MENU', 'system:menu:list',      '/system/menu',   'system/menu/index',   'MenuOutlined',      3),
(14, 2, '部门管理', 'MENU', 'system:dept:list',      '/system/dept',   'system/dept/index',   'ApartmentOutlined', 4),
(15, 2, '字典管理', 'MENU', 'system:dict:list',      '/system/dict',   'system/dict/index',   'DatabaseOutlined',  5),
(16, 2, '操作日志', 'MENU', 'system:log:view',       '/system/log',    'system/log/index',    'FileSearchOutlined',6),
-- 人力资源子菜单
(21, 3, '员工管理', 'MENU', 'hrm:employee:list',  '/hrm/employee','hrm/employee/index','IdcardOutlined',   1),
(22, 3, '部门管理', 'MENU', 'hrm:department:list','/hrm/dept',   'hrm/dept/index',   'ClusterOutlined',   2),
(23, 3, '考勤管理', 'MENU', 'hrm:attendance:list','/hrm/attendance','hrm/attendance/index','ClockCircleOutlined',3),
(24, 3, '薪资管理', 'MENU', 'hrm:salary:list',    '/hrm/salary', 'hrm/salary/index', 'DollarOutlined',    4),
-- 办公自动化子菜单
(31, 4, '通知公告', 'MENU', 'oa:notice:list',     '/oa/notice',  'oa/notice/index',  'NotificationOutlined',1),
(32, 4, '流程管理', 'MENU', 'oa:process:list',    '/oa/process', 'oa/process/index', 'NodeIndexOutlined', 2),
(33, 4, '日程管理', 'MENU', 'oa:schedule:list',   '/oa/schedule','oa/schedule/index','CalendarOutlined',  3),
-- 个人中心子菜单
(41, 5, '个人信息', 'MENU', NULL,                 '/profile/info','profile/info','ProfileOutlined',     1),
(42, 5, '修改密码', 'MENU', NULL,                 '/profile/pwd','profile/pwd','LockOutlined',          2);

-- 5.6 用户数据（BCrypt加密密码）

-- 密码 admin123 的 BCrypt 哈希
INSERT INTO system_user (id, username, password, nickname, email, phone, gender, status, tenant_id) VALUES
(1,  'admin',        '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iAt6Z5Eh', '平台管理员', 'admin@meession.com',       '13800000000', 1, 1, 0),
(2,  'tenant-admin', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iAt6Z5Eh', '租户管理员', 'tenant@meession.com',      '13800000001', 1, 1, 1),
(3,  'teacher',      '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iAt6Z5Eh', '张老师',     'teacher@meession.com',      '13800000002', 1, 1, 1),
(4,  'student01',    '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iAt6Z5Eh', '学生01',     'student01@meession.com',    '13800000003', 1, 1, 1),
(5,  'student02',    '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iAt6Z5Eh', '学生02',     'student02@meession.com',    '13800000004', 2, 1, 1),
(6,  'student03',    '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iAt6Z5Eh', '学生03',     'student03@meession.com',    '13800000005', 1, 1, 1),
(7,  'student04',    '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iAt6Z5Eh', '学生04',     'student04@meession.com',    '13800000006', 2, 1, 1),
(8,  'student05',    '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iAt6Z5Eh', '学生05',     'student05@meession.com',    '13800000007', 1, 1, 1),
(9,  'student06',    '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iAt6Z5Eh', '学生06',     'student06@meession.com',    '13800000008', 1, 1, 1),
(10, 'student07',    '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iAt6Z5Eh', '学生07',     'student07@meession.com',    '13800000009', 2, 1, 1),
(11, 'student08',    '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iAt6Z5Eh', '学生08',     'student08@meession.com',    '13800000010', 1, 1, 1),
(12, 'student09',    '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iAt6Z5Eh', '学生09',     'student09@meession.com',    '13800000011', 1, 1, 1),
(13, 'student10',    '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iAt6Z5Eh', '学生10',     'student10@meession.com',    '13800000012', 2, 1, 1),
(14, 'test01',       '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iAt6Z5Eh', '测试用户01', 'test01@meession.com',       '13800000013', 1, 1, 1),
(15, 'test02',       '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iAt6Z5Eh', '测试用户02', 'test02@meession.com',       '13800000014', 1, 1, 1),
(16, 'test03',       '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iAt6Z5Eh', '测试用户03', 'test03@meession.com',       '13800000015', 1, 1, 1),
(17, 'test04',       '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iAt6Z5Eh', '测试用户04', 'test04@meession.com',       '13800000016', 1, 1, 1),
(18, 'test05',       '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iAt6Z5Eh', '测试用户05', 'test05@meession.com',       '13800000017', 1, 1, 1);

-- 5.7 用户角色关联
INSERT INTO system_user_role (user_id, role_id) VALUES
(1, 1),   -- admin -> 平台管理员
(2, 2),   -- tenant-admin -> 租户管理员
(3, 3),   -- teacher -> 教师
(4, 4),   -- student01 -> 学生
(5, 4),   -- student02 -> 学生
(6, 4),   -- student03 -> 学生
(7, 4),   -- student04 -> 学生
(8, 4),   -- student05 -> 学生
(9, 4),   -- student06 -> 学生
(10, 4),  -- student07 -> 学生
(11, 4),  -- student08 -> 学生
(12, 4),  -- student09 -> 学生
(13, 4),  -- student10 -> 学生
(14, 5),  -- test01 -> 测试用户
(15, 5),  -- test02 -> 测试用户
(16, 5),  -- test03 -> 测试用户
(17, 5),  -- test04 -> 测试用户
(18, 5);  -- test05 -> 测试用户

-- 5.8 角色菜单关联
-- 平台管理员拥有所有菜单
INSERT INTO system_role_menu (role_id, menu_id)
SELECT 1, id FROM system_menu;

-- 租户管理员（排除菜单管理）
INSERT INTO system_role_menu (role_id, menu_id) VALUES
(2, 1), (2, 2), (2, 3), (2, 4), (2, 5),
(2, 11), (2, 12), (2, 14), (2, 15), (2, 16),
(2, 21), (2, 22), (2, 23), (2, 24),
(2, 31), (2, 32), (2, 33),
(2, 41), (2, 42);

-- 教师角色
INSERT INTO system_role_menu (role_id, menu_id) VALUES
(3, 1), (3, 3), (3, 4), (3, 5),
(3, 21), (3, 22), (3, 23), (3, 24),
(3, 31), (3, 32), (3, 33),
(3, 41), (3, 42);

-- 学生角色
INSERT INTO system_role_menu (role_id, menu_id) VALUES
(4, 1), (4, 4), (4, 5),
(4, 23), (4, 31), (4, 32), (4, 33),
(4, 41), (4, 42);

-- 测试用户
INSERT INTO system_role_menu (role_id, menu_id) VALUES
(5, 1), (5, 4), (5, 5),
(5, 31), (5, 32), (5, 33),
(5, 41), (5, 42);

-- 5.9 部门初始数据
INSERT INTO system_dept (id, parent_id, dept_name, dept_code, leader, tenant_id, sort_order) VALUES
(1, 0, '总公司',  'HQ',      'admin',        1, 1),
(2, 1, '技术部',  'TECH',    'admin',        1, 2),
(3, 1, '人事部',  'HR',      'tenant-admin', 1, 3),
(4, 1, '财务部',  'FINANCE', 'tenant-admin', 1, 4),
(5, 1, '教学部',  'EDU',     'teacher',      1, 5);

-- 5.10 员工数据
INSERT INTO hrm_employee (emp_no, emp_name, user_id, dept_id, gender, phone, email, education, emp_status, position, tenant_id) VALUES
('EMP001', '平台管理员', 1,  2, 1, '13800000000', 'admin@meession.com',       'MASTER',   'IN_SERVICE', '系统管理员', 1),
('EMP002', '租户管理员', 2,  3, 1, '13800000001', 'tenant@meession.com',      'BACHELOR', 'IN_SERVICE', 'HR经理',     1),
('EMP003', '张老师',     3,  5, 1, '13800000002', 'teacher@meession.com',      'MASTER',   'IN_SERVICE', '讲师',       1),
('EMP004', '学生01',     4,  5, 1, '13800000003', 'student01@meession.com',    'BACHELOR', 'IN_SERVICE', '学生',       1),
('EMP005', '学生02',     5,  5, 2, '13800000004', 'student02@meession.com',    'BACHELOR', 'IN_SERVICE', '学生',       1),
('EMP006', '学生03',     6,  5, 1, '13800000005', 'student03@meession.com',    'BACHELOR', 'IN_SERVICE', '学生',       1);

-- ----------------------------------------------------------
-- 6. 初始化完成
-- ----------------------------------------------------------
SELECT 'MITEDTSM 数据库初始化完成！' AS message;
SELECT '默认账号: admin / admin123' AS tips;
SELECT '请立即修改所有默认密码！' AS warning;
