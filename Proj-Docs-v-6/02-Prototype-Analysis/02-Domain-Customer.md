# 客户域 (Customer) 分析

## 1. 子模块

### 1.1 客户管理
- 客户列表 (全部/公海/垃圾客户)
- 创建客户 / 新增客户
- 客户详情 (基本信息+附加信息+联系人Tab+商机Tab+订单Tab)
- 客户查重
- 客户转移 / 移入公海 / 领取
- 客户星级 / 来源 / 状态 / 行业 / 城市

### 1.2 联系人管理
- 联系人列表
- 联系人详情 (关联客户)
- 添加联系人

### 1.3 客户分析
- 客户属性分析
- 客户成交率分析
- 客户成交TOP10
- 客户区域分布 (国家/省份)

## 2. 核心实体

### Customer (客户)

| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT | PK |
| name | VARCHAR(100) | 客户名称 |
| type | VARCHAR(20) | 客户类型 |
| industry | VARCHAR(50) | 所属行业 |
| source | VARCHAR(30) | 客户来源 (广告/介绍/自拓/网销) |
| status | VARCHAR(20) | 客户状态 (潜在/意向/成交/流失) |
| starRating | TINYINT | 客户星级 (1-5) |
| city | VARCHAR(50) | 所在城市 |
| address | VARCHAR(255) | 详细地址 |
| ownerId | BIGINT | 客户归属/负责人员 |
| inSea | BOOLEAN | 是否在公海 |
| notes | TEXT | 备注 |
| tenantId | BIGINT | 租户ID |

### Contact (联系人)

| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT | PK |
| customerId | BIGINT | 关联客户 |
| name | VARCHAR(50) | 姓名 |
| mobile | VARCHAR(20) | 手机 |
| phone | VARCHAR(20) | 座机 |
| email | VARCHAR(100) | 邮箱 |
| qq | VARCHAR(20) | QQ |
| wechat | VARCHAR(50) | 微信 |
| department | VARCHAR(50) | 部门 |
| position | VARCHAR(50) | 职务 |
| birthday | DATE | 生日 |
| role | VARCHAR(30) | 角色 |
| lastContactTime | DATETIME | 最后联系时间 |

## 3. 业务规则

1. **公海机制**: 一定时间未跟进的客户自动掉入公海，其他人员可领取
2. **客户转移**: 客户归属可在人员间转移
3. **客户查重**: 创建客户时检查重复（名称/手机）
4. **客户等级**: 根据成交金额/星级自动分级

## 4. 与MITEDTSM的关系

| 能力 | 复用来源 | 复用度 |
|------|---------|--------|
| 基础CRUD | 新建 | 需从零开发 |
| 租户隔离 | TenantBaseDO | 100% |
| 用户/负责人员 | AdminUserDO | 90% |
| 数据字典 | system_dict | 90% |
| 部门归属 | DeptDO | 80% |
| 操作日志 | BizLog SDK | 100% |
