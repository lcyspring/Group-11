# API 规范分析

## 1. REST API 设计

### 1.1 URL 路径规范

```
/admin-api/{module}/{resource}   # 管理后台接口
/app-api/{module}/{resource}     # 移动端/商城接口
/public-api/{module}/{resource}  # 公开接口 (无需认证)
```

示例:
- `/admin-api/system/user/page` - 用户分页查询
- `/admin-api/system/user/create` - 创建用户
- `/admin-api/crm/customer/page` - 客户分页查询

### 1.2 HTTP 方法

| 方法 | 用途 | 示例 |
|------|------|------|
| GET | 查询 | `/admin-api/system/user/get?id=1` |
| POST | 新增/复杂查询 | `/admin-api/system/user/create` |
| PUT | 修改 | `/admin-api/system/user/update` |
| DELETE | 删除 | `/admin-api/system/user/delete?id=1` |

### 1.3 统一返回格式

```json
{
  "code": 0,        // 0=成功, 非0=错误码
  "msg": "操作成功",
  "data": { ... }   // 业务数据
}
```

分页返回:
```json
{
  "code": 0,
  "data": {
    "list": [...],
    "total": 100
  }
}
```

## 2. 认证方式

- **Header**: `Authorization: Bearer {accessToken}`
- **Token 过期**: 返回 401，前端自动用 refreshToken 刷新
- **多终端**: 用户类型枚举 `UserTypeEnum` (ADMIN/MEMBER)

## 3. 参数校验

- Controller 层使用 `@Valid` + JSR-303 注解
- 全局异常处理 `GlobalExceptionHandler`
- 业务异常使用 `ServiceException`

## 4. API 加密

- **Web 前端**: AES 加密 (可配置开关)
  - Header: `X-Api-Encrypt: AES`
  - Request Key / Response Key 独立配置
- **Mobile**: HTTP + RSA 混合加密

## 5. API 文档

- 使用 SpringDoc + Knife4j
- 文档地址: `/doc.html` (开发环境)
- 接口分组按模块划分

## 6. 跨模块调用

### 内部 API 接口模式
```java
// mitedtsm-common 定义通用接口
public interface OAuth2TokenCommonApi {
    OAuth2AccessTokenDO getAccessToken(String accessToken);
}

// 各模块通过注入 API 接口调用
@Resource
private OAuth2TokenCommonApi oauth2TokenApi;
```

### Feign 风格 API (module-api 子模块)
```java
// 在各模块的 api/ 子模块中定义
public interface CustomerApi {
    CustomerDTO getCustomer(Long id);
}
```

## 7. 对 CRM 系统的 API 建议

1. URL 前缀: `/admin-api/crm/` 和 `/app-api/crm/`
2. 统一使用项目现有返回格式 (CommonResult)
3. 分页使用 MyBatis-Plus Page 对象
4. 复杂查询使用 POST + RequestBody
5. 操作日志使用 `@BizLog` 注解
6. 权限控制使用 `@PreAuthorize` + permission 标识
