# 账号初始化方案

> 项目: MITEDTSM (密讯ETM系统)
> 适用对象: 项目组全体
> 版本: v1.0
> 更新日期: 2026-03-17

---

## 一、概述

MITEDTSM 企业管理系统采用多租户架构，区分平台级管理员和租户级管理员。本文档定义系统初始化和各角色的默认账号、权限和菜单。

---

## 二、默认账号体系

### 2.1 账号一览表

| 序号 | 角色 | 用户名 | 密码 | 说明 |
|------|------|--------|------|------|
| 1 | 平台管理员 | admin | admin123 | 超级管理员，管理所有租户 |
| 2 | 租户管理员 | tenant-admin | admin123 | 所属租户的管理员 |
| 3 | 教师 | teacher | teacher123 | 教师角色 |
| 4 | 学生01 | student01 | student123 | 学生账号 |
| 5 | 学生02 | student02 | student123 | 学生账号 |
| 6 | 学生03 | student03 | student123 | 学生账号 |
| 7 | 学生04 | student04 | student123 | 学生账号 |
| 8 | 学生05 | student05 | student123 | 学生账号 |
| 9 | 学生06 | student06 | student123 | 学生账号 |
| 10 | 学生07 | student07 | student123 | 学生账号 |
| 11 | 学生08 | student08 | student123 | 学生账号 |
| 12 | 学生09 | student09 | student123 | 学生账号 |
| 13 | 学生10 | student10 | student123 | 学生账号 |
| 14 | 测试用户01 | test01 | test123 | 测试账号 |
| 15 | 测试用户02 | test02 | test123 | 测试账号 |
| 16 | 测试用户03 | test03 | test123 | 测试账号 |
| 17 | 测试用户04 | test04 | test123 | 测试账号 |
| 18 | 测试用户05 | test05 | test123 | 测试账号 |

> **安全提示**：所有密码均为 BCrypt 加密存储。上线前必须修改默认密码。

### 2.2 登录信息（当前项目）

本项目 Web Admin 默认登录信息：

| 配置项 | 值 |
|------|-----|
| 登录地址 | http://localhost:3000 |
| 默认租户 | 密讯科技 |
| 默认账号 | admin |
| 默认密码 | admin123 |

> 配置来源: `Code/Web/.env` 中的 `VITE_APP_DEFAULT_LOGIN_TENANT`、`VITE_APP_DEFAULT_LOGIN_USERNAME`、`VITE_APP_DEFAULT_LOGIN_PASSWORD`

---

## 三、角色初始化

### 3.1 角色定义

| 角色编码 | 角色名称 | 角色级别 | 数据范围 | 说明 |
|----------|----------|----------|----------|------|
| ROLE_SUPER_ADMIN | 平台管理员 | 平台级 | 全部数据 | 系统最高权限 |
| ROLE_TENANT_ADMIN | 租户管理员 | 租户级 | 本租户数据 | 管理本租户 |
| ROLE_TEACHER | 教师 | 租户级 | 本部门数据 | 教师权限 |
| ROLE_STUDENT | 学生 | 租户级 | 本人数据 | 学生权限 |
| ROLE_TESTER | 测试用户 | 租户级 | 本人数据 | 测试专用 |

### 3.2 角色层级

```
平台管理员 (ROLE_SUPER_ADMIN)
    │
    └── 租户管理员 (ROLE_TENANT_ADMIN)
            │
            ├── 教师 (ROLE_TEACHER)
            │
            └── 学生 (ROLE_STUDENT)
```

---

## 四、权限初始化

### 4.1 权限编码规范

权限编码格式：`模块:资源:操作`

示例：`system:user:list`、`crm:customer:add`

> 注意：本项目使用 `@PreAuthorize` 注解进行权限控制，权限编码需与 `system_menu` 表的 `permission` 字段一致。

### 4.2 权限矩阵

#### 系统管理模块 (system)

| 权限编码 | 权限名称 | 超管 | 租户管理员 | 教师 | 学生 |
|----------|----------|:----:|:--------:|:----:|:----:|
| system:user:list | 用户列表 | ✅ | ✅ | ❌ | ❌ |
| system:user:add | 添加用户 | ✅ | ✅ | ❌ | ❌ |
| system:user:edit | 编辑用户 | ✅ | ✅ | ❌ | ❌ |
| system:user:delete | 删除用户 | ✅ | ❌ | ❌ | ❌ |
| system:role:list | 角色列表 | ✅ | ✅ | ❌ | ❌ |
| system:role:assign | 分配角色 | ✅ | ✅ | ❌ | ❌ |
| system:menu:list | 菜单列表 | ✅ | ❌ | ❌ | ❌ |
| system:menu:manage | 菜单管理 | ✅ | ❌ | ❌ | ❌ |
| system:dict:list | 字典列表 | ✅ | ✅ | ❌ | ❌ |
| system:log:view | 查看日志 | ✅ | ✅ | ❌ | ❌ |

#### 人力资源模块 (hrm)

| 权限编码 | 权限名称 | 超管 | 租户管理员 | 教师 | 学生 |
|----------|----------|:----:|:--------:|:----:|:----:|
| hrm:employee:list | 员工列表 | ✅ | ✅ | ✅ | ❌ |
| hrm:employee:add | 添加员工 | ✅ | ✅ | ❌ | ❌ |
| hrm:employee:edit | 编辑员工 | ✅ | ✅ | ❌ | ❌ |
| hrm:employee:delete | 删除员工 | ✅ | ✅ | ❌ | ❌ |
| hrm:department:list | 部门列表 | ✅ | ✅ | ✅ | ❌ |
| hrm:attendance:list | 考勤列表 | ✅ | ✅ | ✅ | ✅ |
| hrm:attendance:checkin | 打卡签到 | ✅ | ✅ | ✅ | ✅ |

#### 办公自动化模块 (oa)

| 权限编码 | 权限名称 | 超管 | 租户管理员 | 教师 | 学生 |
|----------|----------|:----:|:--------:|:----:|:----:|
| oa:notice:list | 通知列表 | ✅ | ✅ | ✅ | ✅ |
| oa:notice:send | 发送通知 | ✅ | ✅ | ✅ | ❌ |
| oa:process:start | 发起流程 | ✅ | ✅ | ✅ | ✅ |
| oa:process:approve | 审批流程 | ✅ | ✅ | ✅ | ❌ |
| oa:process:list | 流程列表 | ✅ | ✅ | ✅ | ✅ |
| oa:schedule:list | 日程列表 | ✅ | ✅ | ✅ | ✅ |
| oa:schedule:manage | 管理日程 | ✅ | ✅ | ✅ | ✅ |

---

## 五、菜单初始化

### 5.1 菜单树结构

```
MITEDTSM 管理系统
│
├── 首页
│   └── 工作台
│
├── 系统管理 (system)
│   ├── 用户管理
│   ├── 角色管理
│   ├── 菜单管理
│   ├── 部门管理
│   ├── 字典管理
│   └── 操作日志
│
├── 人力资源 (hrm)
│   ├── 员工管理
│   ├── 部门管理
│   ├── 考勤管理
│   │   ├── 考勤记录
│   │   └── 打卡签到
│   └── 薪资管理
│
├── 办公自动化 (oa)
│   ├── 通知公告
│   ├── 流程管理
│   │   ├── 发起流程
│   │   ├── 我的待办
│   │   ├── 我的已办
│   │   └── 流程跟踪
│   ├── 日程管理
│   └── 会议管理
│
└── 个人中心
    ├── 个人信息
    ├── 修改密码
    └── 我的消息
```

### 5.2 菜单可见性

| 菜单 | 平台管理员 | 租户管理员 | 教师 | 学生 |
|------|:--------:|:--------:|:----:|:----:|
| 首页 | ✅ | ✅ | ✅ | ✅ |
| 系统管理 | ✅ | ✅ | ❌ | ❌ |
| 人力资源 | ✅ | ✅ | ✅ | ✅ |
| 办公自动化 | ✅ | ✅ | ✅ | ✅ |
| 个人中心 | ✅ | ✅ | ✅ | ✅ |

---

## 六、字典初始数据

### 6.1 字典类型

| 字典类型 | 字典名称 | 说明 |
|----------|----------|------|
| sys_user_status | 用户状态 | 正常/禁用 |
| sys_gender | 性别 | 男/女/未知 |
| hrm_edu_level | 学历 | 专科/本科/硕士/博士 |
| hrm_emp_status | 员工状态 | 在职/离职/实习 |
| oa_process_status | 流程状态 | 草稿/审批中/已通过/已驳回 |
| oa_notice_type | 通知类型 | 系统通知/公告/消息 |

---

## 七、初始化执行顺序

1. 创建数据库 `mitedtsm_database`
2. 执行 MySQL DDL (建表)
3. 执行各模块 DDL
4. 插入字典数据
5. 插入角色数据
6. 插入菜单数据
7. 插入用户数据
8. 分配角色-用户关联
9. 分配权限-角色关联
10. 分配菜单-角色关联

> **注意**：
> - 完整 SQL 见 [11-init_db.sql](./11-init_db.sql)
> - 本项目数据库初始化 SQL 位于 `InstallPackage/database/base/`
> - 增量升级 SQL 位于 `InstallPackage/database/upgrade/` 或 `InstallPackage/database/new/`

---

## 八、与项目的对应关系

### 后端权限注解

```java
// 示例: 用户列表接口
@PreAuthorize("@ss.hasPermission('system:user:list')")
@GetMapping("/list")
public CommonResult<PageResult<UserVO>> list(UserPageQuery query) { ... }
```

### system_menu 表权限字段

修改菜单权限时，需同步更新:
1. `system_menu` 表的 `permission` 字段
2. 后端 Controller 中的 `@PreAuthorize` 注解
3. 前端动态路由从 `/admin-api/system/auth/get-permission-info` 加载

---

## 参考资料

- [数据库初始化 SQL](./11-init_db.sql)
- [角色权限矩阵](./03-Dependencies-Matrix.md)
- [项目 CLAUDE.md](../../CLAUDE.md)
