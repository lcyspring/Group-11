# 代码规范分析

## 1. Java 代码规范

### 1.1 包命名
```
com.meession.etm.module.{moduleName}
├── controller/
│   ├── admin/      # 管理后台 API
│   └── app/        # 移动端 API
├── service/
│   ├── XxxService.java
│   └── XxxServiceImpl.java
├── convert/        # MapStruct 转换器
├── dal/
│   ├── dataobject/ # DO 类
│   └── mapper/     # MyBatis Mapper
├── enums/          # 枚举
├── job/            # 定时任务
└── util/           # 工具类
```

### 1.2 类命名规范

| 类型 | 命名 | 示例 |
|------|------|------|
| Controller | XxxController | CustomerController |
| Service | XxxService / XxxServiceImpl | CustomerService |
| Mapper | XxxMapper | CustomerMapper |
| DataObject | XxxDO | CustomerDO |
| DTO | XxxDTO | CustomerCreateDTO |
| VO | XxxVO | CustomerVO |
| Convert | XxxConvert | CustomerConvert |
| Enum | XxxEnum | CustomerTypeEnum |

### 1.3 DO 类规范
```java
@TableName("crm_customer")
@KeySequence("...")  // 非MySQL时需要
@Data
@EqualsAndHashCode(callSuper = true)
public class CustomerDO extends TenantBaseDO {
    @TableId(type = IdType.AUTO)
    private Long id;
    // ... fields
}
```

### 1.4 注解使用
- `@PreAuthorize("@ss.hasPermission('crm:customer:create')")` - 权限控制
- `@DataPermission` - 数据权限
- `@TenantIgnore` - 忽略租户隔离
- `@BizLog` - 操作日志

## 2. 数据库规范

### 2.1 建表SQL模板
```sql
CREATE TABLE `crm_customer` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `name` VARCHAR(100) NOT NULL,
  `tenant_id` BIGINT NOT NULL DEFAULT 0,
  `create_by` VARCHAR(64) DEFAULT NULL,
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_by` VARCHAR(64) DEFAULT NULL,
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted` TINYINT NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  KEY `idx_tenant_id` (`tenant_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
```

### 2.2 强制规范
- 使用 MySQL 语法 (`AUTO_INCREMENT`, 不使用 `SEQUENCE`)
- 引擎 InnoDB, 字符集 utf8mb4
- 所有业务表必须包含 `tenant_id` 字段
- 逻辑删除使用 `deleted` 字段

## 3. 前端规范

### 3.1 Vue 组件命名
- 视图组件: PascalCase (如 `CustomerList.vue`)
- 目录结构: `views/crm/customer/`

### 3.2 API 调用
```typescript
// src/api/crm/customer.ts
import { request } from '@/utils/request'

export function getCustomerPage(params: any) {
  return request.get('/admin-api/crm/customer/page', { params })
}
```

### 3.3 注释语言
- Java 代码注释使用中文
- 修改标记: `// [ADD START]`, `// [DELETE]`, `// [MODIFY]` 附带日期和作者
