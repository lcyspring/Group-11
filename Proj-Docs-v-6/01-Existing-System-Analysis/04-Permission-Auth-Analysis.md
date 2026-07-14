# 权限与认证分析

## 1. 认证体系

### 1.1 OAuth2 Token 模式
- **accessToken**: 短期令牌（默认过期，可配置）
- **refreshToken**: 长期令牌，用于刷新 accessToken
- 多终端支持: Web/Android/iOS/微信小程序

### 1.2 登录方式
- 账号密码登录
- 短信验证码登录
- 社交登录（JustAuth: 微信/企业微信/钉钉/飞书）
- 扫码登录

### 1.3 Token 存储
- system_oauth2_access_token 表
- system_oauth2_refresh_token 表
- system_oauth2_client 表 (客户端注册)

## 2. 权限模型 (RBAC)

```
用户 (AdminUserDO)
  ├── 角色 (RoleDO) [多对多]
  │     ├── 菜单权限 (system_menu)
  │     └── 按钮权限 (增/删/改/查/导入/导出)
  └── 部门 (DeptDO) [多对一]
        └── 数据权限范围
```

### 2.1 菜单权限
- `system_menu` 表: 菜单树 + 权限标识 (permission 字段)
- 前端动态路由: 从 `/admin-api/system/auth/get-permission-info` 获取
- 后端注解: `@PreAuthorize("@ss.hasPermission('xxx')")`

### 2.2 数据权限
- `@DataPermission` 注解
- 数据范围: 全部/本部门及子部门/本部门/仅本人
- MyBatis 拦截器自动拼接条件

## 3. 菜单与 i18n

```sql
-- system_menu: 菜单定义
CREATE TABLE system_menu (
  id BIGINT AUTO_INCREMENT,
  name VARCHAR(50),        -- 菜单名称
  permission VARCHAR(100), -- 权限标识
  type TINYINT,            -- 1=目录 2=菜单 3=按钮
  parent_id BIGINT,        -- 父级ID
  sort INT,                -- 排序
  ...
);

-- system_menu_i18n: 菜单国际化
CREATE TABLE system_menu_i18n (
  id BIGINT AUTO_INCREMENT,
  menu_id BIGINT,
  lang VARCHAR(10),        -- zh-CN/en-US/ar-SA...
  name VARCHAR(100),       -- 翻译后的菜单名
  ...
);
```

## 4. 对 HR 系统的复用评估

| 能力 | 复用度 | 说明 |
|------|--------|------|
| Token 认证 | 100% | 直接复用 |
| RBAC 权限模型 | 90% | 复用，扩展 HR 特定权限 |
| 动态路由 | 90% | 复用前端路由加载机制 |
| 数据权限 | 80% | 复用 @DataPermission，扩展公司级隔离 |
| 菜单i18n | 100% | 直接复用 |
| 登录方式 | 100% | 复用所有登录方式 |
