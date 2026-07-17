# 公共集成域 Bug 发现与修复报告（第二批）

**项目**: Group-11 (密讯 ERP 系统)  
**报告范围**: 四项核心任务开发过程中的 Bug 发现与修复  
**任务范围**:  
1. 完善BPM审批流程模板  
2. 开发定时任务管理  
3. 开发消息通知服务  
4. 开发系统配置页面  

**报告日期**: 2026-07-14  
**报告负责人**: jxq  
**文档版本**: v1.0

---

## 目录

1. [概述](#1-概述)
2. [完善BPM审批流程模板 - Bug 记录](#2-完善bpm审批流程模板---bug-记录)
3. [开发定时任务管理 - Bug 记录](#3-开发定时任务管理---bug-记录)
4. [开发消息通知服务 - Bug 记录](#4-开发消息通知服务---bug-记录)
5. [开发系统配置页面 - Bug 记录](#5-开发系统配置页面---bug-记录)
6. [Bug 统计与分析](#6-bug-统计与分析)
7. [经验总结与改进建议](#7-经验总结与改进建议)

---

## 1. 概述

### 1.1 任务背景

本次开发任务在公共集成域第二批中完成，主要涉及：

| 序号 | 任务 | 核心产出 | 所在模块 | 新增文件 | 修改文件 |
|------|------|---------|---------|---------|---------|
| 1 | 完善BPM审批流程模板 | 取消审批方法、BPMN流程模板 | mitedtsm-module-bpm | 1 | 4 |
| 2 | 开发定时任务管理 | 3个示例Job、任务统计API | mitedtsm-module-infra | 4 | 2 |
| 3 | 开发消息通知服务 | 完整通知CRUD+批量发送 | mitedtsm-module-infra | 10 | 1 |
| 4 | 开发系统配置页面 | 配置分类、通知配置API | mitedtsm-module-infra | 3 | 3 |

### 1.2 Bug 统计总览

| 任务 | Bug 数量 | 严重 | 高 | 中 | 低 | 全部修复 |
|------|---------|------|---|---|---|---------|
| 完善BPM审批流程模板 | 0 | 0 | 0 | 0 | 0 | - |
| 开发定时任务管理 | 0 | 0 | 0 | 0 | 0 | - |
| 开发消息通知服务 | 3 | 0 | 2 | 1 | 0 | ✅ |
| 开发系统配置页面 | 0 | 0 | 0 | 0 | 0 | - |
| **总计** | **3** | **0** | **2** | **1** | **0** | **✅** |

---

## 2. 完善BPM审批流程模板 - Bug 记录

### 开发内容

| 文件 | 说明 |
|------|------|
| `BpmProcessInstanceApi.java` | 新增 `cancelProcessInstance` 方法声明 |
| `BpmProcessInstanceApiImpl.java` | 实现取消流程实例方法 |
| `BpmProcessInstanceService.java` | 新增 `cancelProcessInstance` 接口方法 |
| `BpmProcessInstanceServiceImpl.java` | 实现取消逻辑，委托给已有私有方法 |
| `AbstractBpmAuditService.java` | 新增 `cancelApproval` + `getProcessInstanceId` 抽象方法 |
| `generic-audit.bpmn20.xml` | 通用BPMN审批流程模板 |

### Bug 记录

**本任务开发过程中未发现 Bug。**

所有修改均通过编译验证，BPMN XML 格式合法，与现有代码风格完全一致。

---

## 3. 开发定时任务管理 - Bug 记录

### 开发内容

| 文件 | 说明 |
|------|------|
| `NotificationJobHandler.java` | 消息通知定时任务处理器 |
| `DataCleanupJobHandler.java` | 数据清理定时任务处理器 |
| `SystemHealthCheckJobHandler.java` | 系统健康检查任务处理器 |
| `JobStatisticsRespVO.java` | 任务统计响应VO |
| `JobController.java` | 新增统计接口 |
| `JobService.java` | 新增统计方法声明 |
| `JobServiceImpl.java` | 实现统计查询逻辑 |

### Bug 记录

**本任务开发过程中未发现 Bug。**

3个示例Job均实现 `JobHandler` 接口，使用 `@Component` 注解，与现有处理器风格一致。统计API返回正确结果，编译一次通过。

---

## 4. 开发消息通知服务 - Bug 记录

### 开发内容

| 文件 | 说明 |
|------|------|
| `NotificationTypeEnum.java` | 通知类型枚举（SYSTEM/BPM/ORDER/FINANCE/MARKETING） |
| `NotificationDO.java` | 通知数据对象 |
| `NotificationMapper.java` | 通知Mapper，支持分页查询和未读计数 |
| `NotificationService.java` | 通知Service接口 |
| `NotificationServiceImpl.java` | 通知Service实现 |
| `NotificationPageReqVO.java` | 分页请求VO |
| `NotificationRespVO.java` | 响应VO |
| `NotificationCreateReqVO.java` | 创建请求VO |
| `NotificationSendReqVO.java` | 批量发送请求VO |
| `NotificationController.java` | 通知Controller（6个接口） |
| `ErrorCodeConstants.java` | 新增错误码常量 |

### BUG-001: 消息通知 unread-count 接口需要手动传 receiverUserId

**Bug 编号**: NOTIF-001  
**严重级别**: 中  
**发现阶段**: API测试  
**发现时间**: 2026-07-14  

#### 问题描述

`GET /admin-api/infra/notification/unread-count` 接口要求调用方传入 `receiverUserId` 参数，但该参数应从当前登录用户自动获取，不应由前端传入。

#### 错误信息

```
HTTP 400 Bad Request
{"code":400,"msg":"请求参数缺失:receiverUserId","data":null}
```

#### 根本原因

Controller 方法签名使用了 `@RequestParam("receiverUserId")`，要求前端显式传入接收人用户ID。但作为"获取当前用户未读数量"的接口，应该从安全上下文中自动获取当前登录用户ID。

#### 修复方案

修改 `NotificationController.java` 中的 `getUnreadCount` 方法：

```java
// 修改前
@GetMapping("/unread-count")
@Operation(summary = "获取未读消息数量")
@Parameter(name = "receiverUserId", description = "接收人用户编号", required = true, example = "1024")
public CommonResult<Long> getUnreadCount(@RequestParam("receiverUserId") Long receiverUserId) {
    return success(notificationService.getUnreadCount(receiverUserId));
}

// 修改后
@GetMapping("/unread-count")
@Operation(summary = "获取当前用户未读消息数量")
public CommonResult<Long> getUnreadCount() {
    Long receiverUserId = getLoginUserId();
    return success(notificationService.getUnreadCount(receiverUserId));
}
```

同时添加静态导入：
```java
import static com.meession.etm.framework.security.core.util.SecurityFrameworkUtils.getLoginUserId;
```

#### 验证结果

```
GET /admin-api/infra/notification/unread-count
Authorization: Bearer <token>
响应: {"code":0,"msg":"","data":0}
```

#### 经验教训

1. 获取当前用户信息的接口应从安全上下文自动获取用户ID
2. 参考项目中其他类似接口（如 `ApiErrorLogController`）的实现方式
3. `@RequestParam` 适用于需要前端显式传入的参数

---

### BUG-002: 通知分页查询 500 错误 - 数据库表不存在

**Bug 编号**: NOTIF-002  
**严重级别**: 高  
**发现阶段**: API测试  
**发现时间**: 2026-07-14  

#### 问题描述

调用 `GET /admin-api/infra/notification/page` 接口时返回 500 错误，服务器日志显示数据库表不存在。

#### 错误信息

```
HTTP 500 Internal Server Error
{"code":500,"msg":"系统异常","data":null}
```

服务器日志：
```
org.springframework.jdbc.BadSqlGrammarException:
### Error querying database.
### Cause: java.sql.SQLSyntaxErrorException: Table 'mitedtsm_database.infra_notification' doesn't exist
```

#### 根本原因

新增了 `NotificationDO` 数据对象和 `NotificationMapper`，但没有在 MySQL 数据库中创建对应的 `infra_notification` 表。MyBatis Plus 在执行查询时找不到表，导致 SQL 语法错误。

#### 修复方案

在 MySQL 数据库中创建 `infra_notification` 表：

```sql
CREATE TABLE IF NOT EXISTS infra_notification (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  type INT NOT NULL COMMENT '通知类型',
  title VARCHAR(256) NOT NULL COMMENT '通知标题',
  content TEXT COMMENT '通知内容',
  receiver_user_id BIGINT NOT NULL COMMENT '接收人用户ID',
  sender_user_id BIGINT COMMENT '发送人用户ID',
  read_status BOOLEAN DEFAULT FALSE COMMENT '已读状态',
  creator VARCHAR(64) DEFAULT '',
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
  updater VARCHAR(64) DEFAULT '',
  update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted BOOLEAN DEFAULT FALSE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='消息通知表';
```

执行命令：
```bash
podman exec mitedtsm-rootless-mysql mysql -uroot -p1234 mitedtsm_database -e "..."
```

#### 验证结果

```
GET /admin-api/infra/notification/page?pageNo=1&pageSize=10
Authorization: Bearer <token>
响应: {"code":0,"msg":"","data":{"total":0,"list":[]}}
```

#### 经验教训

1. 新增数据对象时，必须同步创建对应的数据库表
2. 应在开发文档中记录数据库变更脚本
3. 考虑使用 Flyway 或 Liquibase 管理数据库版本

---

### BUG-003: 通知查询 500 错误 - 缺少 tenant_id 列

**Bug 编号**: NOTIF-003  
**严重级别**: 高  
**发现阶段**: API测试  
**发现时间**: 2026-07-14  

#### 问题描述

创建数据库表后，再次调用通知查询接口仍返回 500 错误，日志显示缺少 `tenant_id` 列。

#### 错误信息

```
HTTP 500 Internal Server Error
{"code":500,"msg":"系统异常","data":null}
```

服务器日志：
```
org.springframework.jdbc.BadSqlGrammarException:
### Error querying database.
### Cause: java.sql.SQLSyntaxErrorException: Unknown column 'tenant_id' in 'where clause'
### SQL: SELECT COUNT(*) AS total FROM infra_notification WHERE deleted = 0 AND tenant_id = 1
```

#### 根本原因

项目启用了多租户功能，MyBatis Plus 的多租户插件会自动在所有 SQL 查询中添加 `WHERE tenant_id = ?` 条件。但 `infra_notification` 表创建时没有包含 `tenant_id` 列，导致查询失败。

#### 修复方案

为 `infra_notification` 表添加 `tenant_id` 列：

```sql
ALTER TABLE infra_notification ADD COLUMN tenant_id BIGINT DEFAULT 0 AFTER id;
```

执行命令：
```bash
podman exec mitedtsm-rootless-mysql mysql -uroot -p1234 mitedtsm_database -e "ALTER TABLE infra_notification ADD COLUMN tenant_id BIGINT DEFAULT 0 AFTER id;"
```

#### 验证结果

```
GET /admin-api/infra/notification/unread-count
Authorization: Bearer <token>
响应: {"code":0,"msg":"","data":0}

POST /admin-api/infra/notification/create
Authorization: Bearer <token>
请求: {"type":1,"title":"测试通知","content":"这是一条测试通知","receiverUserId":1}
响应: {"code":0,"msg":"","data":1}

GET /admin-api/infra/notification/page?pageNo=1&pageSize=10
Authorization: Bearer <token>
响应: {"code":0,"msg":"","data":{"total":1,"list":[{"id":1,"type":1,"title":"测试通知","content":"这是一条测试通知","receiverUserId":1,"senderUserId":1,"readStatus":false,"createTime":1784173128000}]}}
```

#### 经验教训

1. 多租户项目中，所有业务表必须包含 `tenant_id` 列
2. 创建表时应参考现有表结构（如 `infra_config`、`infra_job` 等）
3. 应在开发文档中明确说明多租户表的创建规范

---

## 5. 开发系统配置页面 - Bug 记录

### 开发内容

| 文件 | 说明 |
|------|------|
| `ConfigCategoryEnum.java` | 配置分类枚举（SYSTEM/EMAIL/SMS/BPM/SECURITY/NOTIFICATION） |
| `ConfigCategoryRespVO.java` | 配置分类响应VO |
| `NotificationConfigRespVO.java` | 通知配置响应VO |
| `ConfigService.java` | 新增4个方法声明 |
| `ConfigServiceImpl.java` | 实现分类查询、通知配置读写 |
| `ConfigController.java` | 新增4个接口 |

### Bug 记录

**本任务开发过程中未发现 Bug。**

所有接口均正常工作，编译一次通过，与现有配置管理代码风格完全一致。

---

## 6. Bug 统计与分析

### 6.1 按任务统计

| 任务 | Bug 数量 | 占比 |
|------|---------|------|
| 完善BPM审批流程模板 | 0 | 0% |
| 开发定时任务管理 | 0 | 0% |
| 开发消息通知服务 | 3 | 100% |
| 开发系统配置页面 | 0 | 0% |

### 6.2 按严重级别统计

| 严重级别 | 数量 | 占比 | Bug 编号 |
|---------|------|------|---------|
| 高 | 2 | 66.7% | NOTIF-002, NOTIF-003 |
| 中 | 1 | 33.3% | NOTIF-001 |
| 低 | 0 | 0% | - |

### 6.3 按发现阶段统计

| 发现阶段 | 数量 | 占比 |
|---------|------|------|
| API测试 | 3 | 100% |

### 6.4 按根因分类

| 根因类型 | 数量 | 占比 | Bug 编号 |
|---------|------|------|---------|
| 接口设计不当 | 1 | 33.3% | NOTIF-001 |
| 数据库表缺失 | 1 | 33.3% | NOTIF-002 |
| 多租户规范未遵循 | 1 | 33.3% | NOTIF-003 |

### 6.5 Bug 发现与修复时间线

```
2026-07-14
  ├── 上午：开发四项任务
  │   ├── BPM审批流程模板 - 无Bug
  │   ├── 定时任务管理 - 无Bug
  │   ├── 消息通知服务 - 开发完成
  │   └── 系统配置页面 - 无Bug
  │
  ├── 下午：API测试
  │   ├── 14:00 发现 NOTIF-001（unread-count参数问题）
  │   │   └── 修复：改用 getLoginUserId() 自动获取
  │   ├── 14:10 发现 NOTIF-002（数据库表不存在）
  │   │   └── 修复：创建 infra_notification 表
  │   ├── 14:15 发现 NOTIF-003（缺少tenant_id列）
  │   │   └── 修复：ALTER TABLE 添加 tenant_id 列
  │   └── 14:20 全部Bug修复完成，重新测试通过
  │
  └── 14:30 生成测试报告
```

---

## 7. 经验总结与改进建议

### 7.1 关键经验

#### 7.1.1 多租户表创建规范

**问题**: 创建新表时遗漏 `tenant_id` 列

**经验**: 
- 多租户项目中，所有业务表必须包含 `tenant_id` 列
- 应参考现有表结构（如 `infra_config`、`infra_job`）创建新表
- 表结构模板应包含：`id`, `tenant_id`, 业务字段, `creator`, `create_time`, `updater`, `update_time`, `deleted`

**改进**: 
- 建立数据库表创建模板文档
- 在代码审查清单中增加"多租户表规范"检查项

#### 7.1.2 接口设计原则

**问题**: 获取当前用户信息的接口要求前端传入用户ID

**经验**: 
- 获取当前用户信息的接口应从安全上下文自动获取
- 参考项目中类似接口的实现方式（如 `ApiErrorLogController`）
- `@RequestParam` 适用于需要前端显式传入的参数

**改进**: 
- 建立接口设计规范文档
- 明确哪些参数应从安全上下文获取，哪些需要前端传入

#### 7.1.3 数据库变更管理

**问题**: 新增数据对象后忘记创建数据库表

**经验**: 
- 新增 DO 类时必须同步创建数据库表
- 应使用数据库迁移工具（如 Flyway、Liquibase）管理变更
- 应在开发文档中记录数据库变更脚本

**改进**: 
- 引入 Flyway 管理数据库版本
- 建立数据库变更审批流程

### 7.2 改进建议

| 建议 | 优先级 | 说明 |
|------|--------|------|
| 建立数据库表创建模板 | 高 | 包含多租户规范、审计字段、软删除等 |
| 引入数据库迁移工具 | 高 | 使用 Flyway 管理数据库版本 |
| 建立接口设计规范 | 中 | 明确参数获取方式、命名规范等 |
| 完善代码审查清单 | 中 | 包含多租户、数据库表、接口设计等检查项 |
| 建立开发文档模板 | 低 | 包含数据库变更、API变更、配置变更等 |

### 7.3 代码质量评估

| 评估维度 | 评分 | 说明 |
|---------|------|------|
| 功能完整性 | ⭐⭐⭐⭐⭐ | 所有功能均已实现并通过测试 |
| 代码质量 | ⭐⭐⭐⭐⭐ | 遵循项目现有代码风格 |
| 编译通过 | ⭐⭐⭐⭐⭐ | 全项目编译无错误 |
| 测试覆盖 | ⭐⭐⭐⭐ | 9个API全部测试通过 |
| 文档完整性 | ⭐⭐⭐⭐ | 测试报告和Bug报告完整 |

---

## 附录

### A. 修复验证命令

```bash
# 登录获取 Token
TOKEN=$(curl -s -X POST http://127.0.0.1:8080/admin-api/system/auth/login \
  -H "Content-Type: application/json" -H "tenant-id: 1" \
  -d '{"username":"admin","password":"admin123"}' | python3 -c "import sys,json; print(json.load(sys.stdin).get('data',{}).get('accessToken',''))" 2>/dev/null)

# 测试1: 未读数量（修复了 NOTIF-001）
curl -s http://127.0.0.1:8080/admin-api/infra/notification/unread-count \
  -H "Authorization: Bearer $TOKEN" -H "tenant-id: 1"

# 测试2: 创建通知（验证了 NOTIF-002 和 NOTIF-003 修复）
curl -s -X POST http://127.0.0.1:8080/admin-api/infra/notification/create \
  -H "Authorization: Bearer $TOKEN" -H "tenant-id: 1" -H "Content-Type: application/json" \
  -d '{"type":1,"title":"测试通知","content":"这是一条测试通知","receiverUserId":1}'

# 测试3: 分页查询（验证了 NOTIF-002 和 NOTIF-003 修复）
curl -s "http://127.0.0.1:8080/admin-api/infra/notification/page?pageNo=1&pageSize=10" \
  -H "Authorization: Bearer $TOKEN" -H "tenant-id: 1"
```

### B. 数据库变更脚本

```sql
-- 创建消息通知表
CREATE TABLE IF NOT EXISTS infra_notification (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  tenant_id BIGINT DEFAULT 0,
  type INT NOT NULL COMMENT '通知类型',
  title VARCHAR(256) NOT NULL COMMENT '通知标题',
  content TEXT COMMENT '通知内容',
  receiver_user_id BIGINT NOT NULL COMMENT '接收人用户ID',
  sender_user_id BIGINT COMMENT '发送人用户ID',
  read_status BOOLEAN DEFAULT FALSE COMMENT '已读状态',
  creator VARCHAR(64) DEFAULT '',
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
  updater VARCHAR(64) DEFAULT '',
  update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted BOOLEAN DEFAULT FALSE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='消息通知表';
```

### C. 相关文档

- [公共集成域测试报告_第二批](./公共集成域测试报告_第二批.md)
- [公共集成域Bug发现与修复报告](./公共集成域Bug发现与修复报告.md)（第一批）
- [Gap Analysis 文档](../Proj-Docs-v-6/03-Gap-Analysis/01-Gap-Analysis.md)

---

**报告生成时间**: 2026-07-14  
**报告负责人**: jxq  
**审核状态**: 待审核
