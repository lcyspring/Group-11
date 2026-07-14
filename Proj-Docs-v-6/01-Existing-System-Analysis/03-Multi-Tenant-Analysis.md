# 多租户系统分析

## 1. 租户架构

MITEDTSM 采用**共享数据库、共享表、tenant_id 隔离**的多租户方案。

## 2. 核心实现

### 2.1 租户基类

```java
// TenantBaseDO - 所有租户隔离表继承此类
public class TenantBaseDO extends BaseDO {
    private Long tenantId;  // 租户编号
}
```

- **TenantBaseDO**: 用于需要租户隔离的表（如 CRM/ERP 业务数据）
- **BaseDO**: 用于系统级共享表（如 system_menu, system_dict）

### 2.2 租户拦截器

框架层通过 MyBatis-Plus 拦截器自动注入 `tenant_id` 条件：
- 查询: 自动附加 `WHERE tenant_id = ?`
- 插入: 自动填充 `tenant_id` 字段
- 更新/删除: 自动附加 `tenant_id` 条件

### 2.3 忽略租户

`@TenantIgnore` 注解用于需要跨租户访问的场景（如管理员查看全平台数据）。

## 3. 租户上下文

```java
// TenantContextHolder - 线程级租户上下文
TenantContextHolder.getTenantId();    // 获取当前租户
TenantContextHolder.setTenantId(id);  // 设置当前租户
```

- 租户ID从 OAuth2 Token 中解析
- Web 请求通过 Filter 自动设置
- 异步任务通过 Transmittable ThreadLocal 传递

## 4. 租户管理

| 功能 | 位置 | 说明 |
|------|------|------|
| 租户列表 | module-system | 租户CRUD |
| 租户套餐 | module-system | 绑定功能套餐 |
| 租户域名 | module-system | 域名绑定 |
| 租户菜单 | module-system | 按租户分配菜单 |

## 5. 对 HR 系统的影响

### 模式选择
HR 系统需要决定：
- **方案A**: 复用现有租户模式 — 每个客户公司作为一个租户
  - 优点: 直接复用，开发快
  - 缺点: 集团多公司场景不自然
- **方案B**: 扩展为集团多公司 — tenant_id + company_id 双层隔离
  - 优点: 天然支持集团管控
  - 缺点: 需要扩展拦截器

### 建议
根据原型功能（公司架构设置、部门树），建议采用**方案B**：
- 保留 tenant_id 作为 SaaS 租户
- 新增 company_id 作为集团内公司
- 部门关联 company_id
