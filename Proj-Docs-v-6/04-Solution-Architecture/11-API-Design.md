# API 设计文档

## MITEDTSM CRM + OA 子系统

---

## 文档信息

| 项目 | 说明 |
|------|------|
| 项目名称 | MITEDTSM（密讯ETM系统）CRM + OA 子系统 |
| API 风格 | RESTful |
| 文档工具 | Knife4j (OpenAPI 3.0) |
| 基础路径 | `/admin-api/` 和 `/app-api/` |
| 参考来源 | 04-架构设计/API_Design.md |
| 文档版本 | V1.0 |
| 创建日期 | 2026-06-25 |

---

## 1. RESTful API 设计规范

### 1.1 URL 命名规范

| 规范 | 说明 | 示例 |
|------|------|------|
| 使用小写字母 | URL 全部小写 | /admin-api/system/users |
| 使用连字符 | 多单词用 - 分隔 | /admin-api/system/user-roles |
| 资源名用复数 | 集合资源用复数 | /admin-api/crm/customers |
| 层级不超过 3 层 | 避免过深嵌套 | /admin-api/system/depts/{id}/users |
| 管理端前缀 | /admin-api/ | 管理后台 API |
| 用户端前缀 | /app-api/ | 移动端/客户端 API |

### 1.2 HTTP 方法

| 方法 | 用途 | 幂等性 |
|------|------|--------|
| GET | 查询资源 | 是 |
| POST | 创建资源 | 否 |
| PUT | 全量更新资源 | 是 |
| DELETE | 删除资源（逻辑删除） | 是 |

### 1.3 HTTP 状态码

| 状态码 | 说明 |
|--------|------|
| 200 | 请求成功 |
| 201 | 创建成功 |
| 204 | 删除成功（无响应体） |
| 400 | 请求参数错误 |
| 401 | 未认证 |
| 403 | 无权限 |
| 404 | 资源不存在 |
| 409 | 资源冲突 |
| 422 | 业务逻辑错误 |
| 500 | 服务器内部错误 |

---

## 2. 统一响应格式

### 2.1 成功响应

```json
{
  "code": 200,
  "msg": "操作成功",
  "data": {
    "id": 1,
    "username": "admin"
  }
}
```

### 2.2 分页响应

```json
{
  "code": 200,
  "msg": "查询成功",
  "data": {
    "records": [...],
    "total": 100,
    "page": 1,
    "pageSize": 20,
    "totalPages": 5
  }
}
```

### 2.3 错误响应

```json
{
  "code": 40001,
  "msg": "用户名已存在",
  "data": null
}
```

### 2.4 分页请求参数

| 参数 | 类型 | 必填 | 默认值 | 说明 |
|------|------|------|--------|------|
| page | Integer | 否 | 1 | 当前页码 |
| pageSize | Integer | 否 | 20 | 每页条数 |
| sortField | String | 否 | id | 排序字段 |
| sortOrder | String | 否 | desc | 排序方式(asc/desc) |

---

## 3. 核心 API 详细定义

### 3.1 认证模块

#### 3.1.1 用户登录

| 属性 | 值 |
|------|-----|
| 方法 | POST |
| 路径 | /admin-api/system/auth/login |
| 权限 | 无 |
| 描述 | 用户登录，返回 Token |

**请求参数：**

```json
{
  "username": "admin",
  "password": "123456",
  "captchaCode": "abc123",
  "captchaKey": "uuid-xxx"
}
```

**响应示例：**

```json
{
  "code": 200,
  "msg": "登录成功",
  "data": {
    "token": "sa-token-xxx",
    "tokenName": "Authorization",
    "userInfo": {
      "id": 1,
      "username": "admin",
      "nickname": "管理员",
      "avatar": "https://minio.example.com/avatar/1.jpg"
    }
  }
}
```

#### 3.1.2 获取当前用户权限信息

| 属性 | 值 |
|------|-----|
| 方法 | GET |
| 路径 | /admin-api/system/auth/get-permission-info |
| 权限 | 登录即可 |
| 描述 | 获取当前登录用户信息、角色、权限、动态菜单 |

#### 3.1.3 用户登出

| 属性 | 值 |
|------|-----|
| 方法 | POST |
| 路径 | /admin-api/system/auth/logout |
| 权限 | 登录即可 |

---

### 3.2 用户管理模块

#### 3.2.1 分页查询用户列表

| 属性 | 值 |
|------|-----|
| 方法 | GET |
| 路径 | /admin-api/system/user/list |
| 权限 | system:user:list |

**请求参数：**

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| username | String | 否 | 用户名（模糊） |
| nickname | String | 否 | 昵称（模糊） |
| deptId | Long | 否 | 部门 ID |
| status | Integer | 否 | 状态(0-禁用,1-启用) |

#### 3.2.2 创建用户

| 属性 | 值 |
|------|-----|
| 方法 | POST |
| 路径 | /admin-api/system/user/create |
| 权限 | system:user:create |

```json
{
  "username": "zhangsan",
  "password": "123456",
  "nickname": "张三",
  "email": "zhangsan@example.com",
  "phone": "13800138000",
  "deptId": 1,
  "roleIds": [2, 3],
  "status": 1
}
```

---

### 3.3 CRM 模块 API

#### 3.3.1 分页查询客户

| 属性 | 值 |
|------|-----|
| 方法 | GET |
| 路径 | /admin-api/crm/customer/page |
| 权限 | crm:customer:list |

**请求参数：**

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| customerName | String | 否 | 客户名称（模糊） |
| industry | String | 否 | 行业 |
| customerLevel | String | 否 | 客户等级 |
| ownerId | Long | 否 | 负责人 ID |
| inSea | Integer | 否 | 是否公海(0-否,1-是) |

#### 3.3.2 创建客户

| 属性 | 值 |
|------|-----|
| 方法 | POST |
| 路径 | /admin-api/crm/customer/create |
| 权限 | crm:customer:create |

#### 3.3.3 创建商机

| 属性 | 值 |
|------|-----|
| 方法 | POST |
| 路径 | /admin-api/crm/opportunity/create |
| 权限 | crm:opportunity:create |

```json
{
  "customerId": 1,
  "title": "XX公司ERP项目",
  "estimatedAmount": 500000.00,
  "probability": 60,
  "expectedCloseDate": "2026-09-30",
  "stage": "NEGOTIATION"
}
```

#### 3.3.4 推进商机阶段

| 属性 | 值 |
|------|-----|
| 方法 | PUT |
| 路径 | /admin-api/crm/opportunity/{id}/advance-stage |
| 权限 | crm:opportunity:update |

#### 3.3.5 创建订单

| 属性 | 值 |
|------|-----|
| 方法 | POST |
| 路径 | /admin-api/crm/order/create |
| 权限 | crm:order:create |

#### 3.3.6 销售漏斗分析

| 属性 | 值 |
|------|-----|
| 方法 | GET |
| 路径 | /admin-api/crm/report/funnel |
| 权限 | crm:report:funnel |

---

### 3.4 审批模块 API

#### 3.4.1 发起审批

| 属性 | 值 |
|------|-----|
| 方法 | POST |
| 路径 | /admin-api/bpm/process/start |
| 权限 | 登录即可 |

```json
{
  "processKey": "process_leave",
  "businessKey": "LEAVE-20260625-001",
  "variables": { "leaveDays": 3 }
}
```

#### 3.4.2 审批任务

| 属性 | 值 |
|------|-----|
| 方法 | POST |
| 路径 | /admin-api/bpm/task/{taskId}/complete |
| 权限 | 登录即可 |

```json
{
  "approved": true,
  "comment": "同意请假申请"
}
```

#### 3.4.3 查询待办任务

| 属性 | 值 |
|------|-----|
| 方法 | GET |
| 路径 | /admin-api/bpm/task/todo |
| 权限 | 登录即可 |

#### 3.4.4 统一审批中心

| 属性 | 值 |
|------|-----|
| 方法 | GET |
| 路径 | /admin-api/bpm/approval-center |
| 权限 | 登录即可 |
| 描述 | 统一查询所有待审批/已审批/我发起的审批项（跨9类审批类型） |

---

### 3.5 员工管理模块 API

#### 3.5.1 分页查询员工花名册

| 属性 | 值 |
|------|-----|
| 方法 | GET |
| 路径 | /admin-api/employee/page |
| 权限 | employee:list |

#### 3.5.2 员工入职

| 属性 | 值 |
|------|-----|
| 方法 | POST |
| 路径 | /admin-api/employee/entry |
| 权限 | employee:entry |

#### 3.5.3 员工转正

| 属性 | 值 |
|------|-----|
| 方法 | POST |
| 路径 | /admin-api/employee/{id}/regular |
| 权限 | employee:regular |

#### 3.5.4 员工调岗

| 属性 | 值 |
|------|-----|
| 方法 | POST |
| 路径 | /admin-api/employee/{id}/transfer |
| 权限 | employee:transfer |

```json
{
  "toDeptId": 2,
  "toPosition": "高级Java工程师",
  "toLevel": "P6",
  "effectiveDate": "2026-07-01",
  "reason": "业务发展需要"
}
```

#### 3.5.5 员工离职

| 属性 | 值 |
|------|-----|
| 方法 | POST |
| 路径 | /admin-api/employee/{id}/leave |
| 权限 | employee:leave |

---

### 3.6 考勤管理模块 API

#### 3.6.1 查询考勤记录

| 属性 | 值 |
|------|-----|
| 方法 | GET |
| 路径 | /admin-api/attendance/record/page |
| 权限 | attendance:record:list |

**请求参数：**

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| employeeId | Long | 否 | 员工 ID |
| deptId | Long | 否 | 部门 ID |
| startDate | String | 是 | 开始日期 |
| endDate | String | 是 | 结束日期 |
| status | String | 否 | 状态(NORMAL/LATE/LEAVE_EARLY/ABSENT) |

#### 3.6.2 请假申请

| 属性 | 值 |
|------|-----|
| 方法 | POST |
| 路径 | /admin-api/attendance/leave/apply |
| 权限 | attendance:leave:apply |

```json
{
  "leaveType": "ANNUAL",
  "startTime": "2026-07-01 09:00:00",
  "endTime": "2026-07-03 18:00:00",
  "reason": "年假休息"
}
```

#### 3.6.3 批量修正考勤

| 属性 | 值 |
|------|-----|
| 方法 | PUT |
| 路径 | /admin-api/attendance/record/batch-correct |
| 权限 | attendance:record:correct |

---

### 3.7 绩效管理模块 API

#### 3.7.1 创建考核计划

| 属性 | 值 |
|------|-----|
| 方法 | POST |
| 路径 | /admin-api/performance/plan/create |
| 权限 | performance:plan:create |

```json
{
  "planName": "2026年6月月度考核",
  "planType": "MONTHLY",
  "planYear": 2026,
  "planMonth": 6,
  "startDate": "2026-06-01",
  "endDate": "2026-06-30",
  "targetDeptId": 1
}
```

#### 3.7.2 提交考核评分

| 属性 | 值 |
|------|-----|
| 方法 | PUT |
| 路径 | /admin-api/performance/record/{id}/evaluate |
| 权限 | performance:record:evaluate |

```json
{
  "score": 92.5,
  "evaluation": "工作表现优秀，超额完成目标"
}
```

---

### 3.8 薪酬管理模块 API

#### 3.8.1 查询工资表

| 属性 | 值 |
|------|-----|
| 方法 | GET |
| 路径 | /admin-api/salary/record/page |
| 权限 | salary:record:list |

**请求参数：**

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| salaryMonth | String | 是 | 工资月份(YYYY-MM) |
| deptId | Long | 否 | 部门 ID |
| employeeId | Long | 否 | 员工 ID |

#### 3.8.2 员工调薪

| 属性 | 值 |
|------|-----|
| 方法 | POST |
| 路径 | /admin-api/salary/adjust/create |
| 权限 | salary:adjust:create |

```json
{
  "employeeId": 1,
  "adjustType": "RAISE",
  "beforeSalary": 15000.00,
  "afterSalary": 18000.00,
  "effectiveDate": "2026-07-01",
  "reason": "年度调薪，晋升调薪"
}
```

---

### 3.9 办公协作模块 API

#### 3.9.1 工作报告

| 方法 | 路径 | 权限 | 说明 |
|------|------|------|------|
| GET | /admin-api/oa/work-report/page | oa:report:list | 分页查询报告 |
| POST | /admin-api/oa/work-report/create | oa:report:create | 创建报告 |

#### 3.9.2 任务管理

| 方法 | 路径 | 权限 | 说明 |
|------|------|------|------|
| GET | /admin-api/oa/task/page | oa:task:list | 分页查询任务 |
| POST | /admin-api/oa/task/create | oa:task:create | 创建任务 |
| PUT | /admin-api/oa/task/{id}/progress | oa:task:update | 更新进度 |
| PUT | /admin-api/oa/task/{id}/complete | oa:task:update | 完成任务 |

#### 3.9.3 工单管理

| 方法 | 路径 | 权限 | 说明 |
|------|------|------|------|
| GET | /admin-api/oa/work-order/page | oa:workorder:list | 分页查询工单 |
| POST | /admin-api/oa/work-order/create | oa:workorder:create | 创建工单 |
| PUT | /admin-api/oa/work-order/{id}/process | oa:workorder:process | 处理工单 |
| PUT | /admin-api/oa/work-order/{id}/complete | oa:workorder:complete | 完结工单 |
| PUT | /admin-api/oa/work-order/{id}/return | oa:workorder:return | 退回工单 |

---

### 3.10 通用 API

| 方法 | 路径 | 权限 | 说明 |
|------|------|------|------|
| GET | /admin-api/system/dict-data/list | 登录即可 | 查询字典数据 |
| GET | /admin-api/system/dept/tree | 登录即可 | 查询部门树 |
| POST | /admin-api/infra/file/upload | 登录即可 | 上传文件 |
| GET | /admin-api/infra/file/{id}/download | 登录即可 | 下载文件 |
| GET | /admin-api/system/message/page | 登录即可 | 查询我的消息 |
| PUT | /admin-api/system/message/read-all | 登录即可 | 全部已读 |

---

## 4. Knife4j 注解示例

```java
@RestController
@RequestMapping("/admin-api/crm/customer")
@Tag(name = "客户管理", description = "客户的增删改查操作")
public class CustomerController {

    @Operation(summary = "分页查询客户", description = "支持多条件筛选和分页")
    @Parameters({
        @Parameter(name = "customerName", description = "客户名称（模糊）"),
        @Parameter(name = "industry", description = "行业"),
        @Parameter(name = "page", description = "页码", example = "1"),
        @Parameter(name = "pageSize", description = "每页条数", example = "20")
    })
    @SaCheckPermission("crm:customer:list")
    @GetMapping("/page")
    public Result<PageResult<CustomerVO>> page(CustomerPageQuery query) {
        // ...
    }

    @Operation(summary = "创建客户")
    @SaCheckPermission("crm:customer:create")
    @PostMapping("/create")
    public Result<CustomerVO> create(@Valid @RequestBody CustomerCreateDTO dto) {
        // ...
    }
}
```

---

## 5. 错误码定义

| 错误码 | 错误信息 | 说明 |
|--------|----------|------|
| 40001 | 用户名已存在 | 创建用户时用户名重复 |
| 40002 | 角色编码已存在 | 创建角色时编码重复 |
| 40003 | 员工编号已存在 | 创建员工时编号重复 |
| 40004 | 简历已存在 | 手机号或邮箱重复 |
| 40005 | 旧密码不正确 | 修改密码时旧密码错误 |
| 40006 | 客户名称已存在 | 同公司下客户名称重复 |
| 40101 | 用户未登录 | Token 无效或过期 |
| 40102 | 账号已被禁用 | 账号状态为禁用 |
| 40103 | 验证码错误 | 验证码输入错误 |
| 40301 | 无操作权限 | 缺少对应权限标识 |
| 40302 | 无数据权限 | 无权访问该部门数据 |
| 40401 | 用户不存在 | 用户 ID 无效 |
| 40402 | 客户不存在 | 客户 ID 无效 |
| 40403 | 员工不存在 | 员工 ID 无效 |
| 40901 | 审批流程已存在 | 重复发起审批 |
| 40902 | 审批任务已处理 | 任务已被其他人处理 |
| 42201 | 请假天数超出余额 | 假期余额不足 |
| 42202 | 导入数据格式错误 | Excel 模板格式不正确 |
| 42203 | 工资月份已锁定 | 该月工资已确认无法修改 |
| 42204 | 回款金额超限 | 回款金额超出订单未回款余额 |
| 42205 | 客户存在关联业务 | 客户存在商机/订单，无法删除 |
| 42206 | 商机阶段不可后退 | 商机阶段只能向前推进 |
| 50001 | 文件上传失败 | MinIO 服务异常 |
| 50002 | 流程引擎异常 | Flowable 执行异常 |

---

## 6. API 安全策略

### 6.1 认证方式

- 所有 API（除登录、验证码外）需在请求头携带 Token
- Token 格式：`Authorization: Bearer <sa-token-value>`
- Token 有效期：默认 2 小时，支持刷新

### 6.2 鉴权方式

- 接口级权限：通过 `@SaCheckPermission` 注解控制
- 角色级权限：通过 `@SaCheckRole` 注解控制
- 数据权限：通过 MyBatis-Plus 拦截器自动注入 `tenant_id` 和部门过滤条件

### 6.3 接口限流

- 登录接口：每分钟最多 5 次
- 验证码接口：每分钟最多 10 次
- 通用接口：每分钟最多 100 次
- 限流实现：Redis + Lua 脚本

### 6.4 请求防护

- XSS 过滤：全局过滤器过滤特殊字符
- 参数校验：JSR-303 Bean Validation
- API 路径严格区分 `/admin-api/`（管理后台）和 `/app-api/`（移动端/客户端）

---

## 7. 文档变更记录

| 版本 | 日期 | 变更内容 | 变更人 |
|------|------|----------|--------|
| V1.0 | 2026-06-25 | 初始版本，参考 04-架构设计/API_Design.md，适配 mitedtsm 路径规范 | 架构设计团队 |
