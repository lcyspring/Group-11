# 数据库架构分析

## 1. 数据库概览

- **数据库**: MySQL 8.0 (开发/测试/生产统一)
- **引擎**: InnoDB
- **字符集**: utf8mb4 / utf8mb4_unicode_ci
- **主键策略**: BIGINT AUTO_INCREMENT (MySQL)
- **SQL管理**: `/InstallPackage/database/`

## 2. 基础 DO 类层次

```
BaseDO (基础字段)
  ├── id (BIGINT, AUTO_INCREMENT)
  ├── createTime
  ├── updateTime
  ├── createBy (创建者)
  ├── updateBy (更新者)
  └── deleted (逻辑删除标记)

TenantBaseDO extends BaseDO
  └── tenantId (BIGINT, 租户ID)

BaseDO ─ 用于系统共享表 (system_menu, system_dict 等)
TenantBaseDO ─ 用于业务隔离表 (CRM/ERP 等业务数据)
```

## 3. 核心系统表 (module-system)

| 表名 | 说明 |
|------|------|
| system_users | 管理后台用户 |
| system_role | 角色 |
| system_role_menu | 角色-菜单关联 |
| system_menu | 菜单/权限 |
| system_menu_i18n | 菜单国际化 |
| system_dept | 部门 |
| system_post | 岗位 |
| system_dict_type | 字典类型 |
| system_dict_data | 字典数据 |
| system_tenant | 租户信息 |
| system_tenant_package | 租户套餐 |
| system_oauth2_access_token | OAuth2访问令牌 |
| system_oauth2_refresh_token | OAuth2刷新令牌 |
| system_oauth2_client | OAuth2客户端 |
| system_login_log | 登录日志 |
| system_operate_log | 操作日志 |
| system_notice | 通知公告 |
| system_sms_code | 短信验证码 |
| system_sms_log | 短信日志 |
| system_mail_log | 邮件日志 |

## 4. 基础表 (module-infra)

| 表名 | 说明 |
|------|------|
| infra_config | 配置管理 |
| infra_file_config | 文件存储配置 |
| infra_file | 文件记录 |
| infra_job | 定时任务 |
| infra_job_log | 任务日志 |
| infra_data_source_config | 数据源配置 |
| infra_api_access_log | API访问日志 |
| infra_api_error_log | API错误日志 |

## 5. SQL 脚本结构

```
InstallPackage/database/
├── base/           # 基础表结构 DDL
├── upgrade/        # 增量升级脚本
├── test-data/      # 测试数据
├── final/          # 最终初始化数据
└── tmp/            # 临时SQL (开发中)
```

## 6. 多租户实现原理

- MyBatis-Plus 拦截器 `TenantLineInnerInterceptor`
- 查询: 自动附加 `AND tenant_id = ?`
- 插入: 自动填充 `tenant_id`
- 更新/删除: 自动附加条件
- `@TenantIgnore` 注解跳过隔离

## 7. 对 CRM 系统的数据库建议

1. **表名前缀**: 使用 `crm_` 前缀 (如 `crm_customer`, `crm_opportunity`)
2. **主键策略**: 统一使用 BIGINT AUTO_INCREMENT
3. **继承 TenantBaseDO**: 所有业务表必须 tenant_id 隔离
4. **SQL 脚本**: CRM 模块 SQL 放 `database/new/crm/`
5. **菜单数据**: 新增菜单需同时写入 `system_menu` + `system_menu_i18n`
