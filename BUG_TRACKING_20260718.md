# Bug追踪报告 - 2026年7月18日开发内容

## 文档信息

| 项目 | 说明 |
| :--- | :--- |
| **项目名称** | Group-11 CRM系统 |
| **文档版本** | v1.0 |
| **创建日期** | 2026-07-18 |
| **作者** | 开发团队 |
| **状态** | 活跃 |

---

## 开发功能概述

今日共开发两个功能模块：

1. **客户导入导出功能 + 客户星级评估接口**
2. **客户模块与商机模块集成 + 客户模块前端联调**

---

## 目录

1. [严重级Bug](#1-严重级bug)
2. [高级Bug](#2-高级bug)
3. [中级Bug](#3-中级bug)
4. [已解决的历史Bug](#4-已解决的历史bug)
5. [潜在问题与优化建议](#5-潜在问题与优化建议)

---

## 1. 严重级Bug

| Bug ID | 标题 | 状态 | 功能模块 |
| :---: | :--- | :---: | :---: |
| BUG-008 | Vite启动失败，pnpm dev命令无法启动 | ✅ 已修复 | 环境配置 |
| BUG-009 | Maven插件错误，spring-boot:run命令无法执行 | ✅ 已修复 | 环境配置 |

### BUG-008: Vite启动失败，pnpm dev命令无法启动

**问题描述**：执行`pnpm dev`命令无法启动前端开发服务器，报错信息未明确。

**影响范围**：前端项目整体启动

**错误代码**：
```bash
pnpm dev  # 无法启动
```

**修复方案**：
```bash
node node_modules/vite/bin/vite.js --mode env.local  # 直接调用Vite命令启动
```

**涉及文件**：
- `Web/package.json` - 确认scripts配置

---

### BUG-009: Maven插件错误，spring-boot:run命令无法执行

**问题描述**：执行`mvn spring-boot:run`提示`No plugin found for prefix 'spring-boot'`，无法启动后端服务。

**影响范围**：后端项目启动

**错误代码**：
```bash
mvn spring-boot:run  # 报错：No plugin found for prefix 'spring-boot'
```

**修复方案**：检查`mitedtsm-server/pom.xml`确认已引入`spring-boot-maven-plugin`，确保在正确目录执行命令：

```bash
cd Server/mitedtsm-server && mvn spring-boot:run -Dspring-boot.run.profiles=local
```

**涉及文件**：
- `Server/mitedtsm-server/pom.xml`

---

## 2. 高级Bug

| Bug ID | 标题 | 状态 | 功能模块 |
| :---: | :--- | :---: | :---: |
| BUG-010 | 客户详情VO缺少商机统计字段 | ✅ 已修复 | 客户-商机集成 |
| BUG-011 | CustomerController未注入商机服务 | ✅ 已修复 | 客户-商机集成 |

### BUG-010: 客户详情VO缺少商机统计字段

**问题描述**：`CrmCustomerRespVO`缺少商机数量和成交金额字段，无法在客户详情页展示商机统计信息。

**影响范围**：客户详情页头部统计展示

**错误代码**：

```java
// 修复前（缺少字段）
public class CrmCustomerRespVO {
    // ... 其他字段 ...
    // 缺少 businessCount 和 totalDealAmount
}
```

**修复方案**：

```java
// 修复后（新增字段）
public class CrmCustomerRespVO {
    // ... 其他字段 ...
    
    @Schema(description = "商机数量", example = "5")
    private Long businessCount;

    @Schema(description = "成交总金额", example = "1000000")
    private Long totalDealAmount;
}
```

**涉及文件**：
- `Server/mitedtsm-module-crm/src/main/java/com/meession/etm/module/crm/controller/admin/customer/vo/customer/CrmCustomerRespVO.java`

---

### BUG-011: CustomerController未注入商机服务

**问题描述**：`CrmCustomerController`未注入`CrmBusinessService`，导致无法在`buildCustomerDetailList`方法中调用商机统计接口。

**影响范围**：客户详情页商机统计数据展示

**错误代码**：

```java
// 修复前（缺少注入）
@Resource
private CrmCustomerService customerService;
@Resource
private CrmCustomerPoolConfigService customerPoolConfigService;
// 缺少 CrmBusinessService 注入
```

**修复方案**：

```java
// 修复后（新增注入）
@Resource
private CrmCustomerService customerService;
@Resource
private CrmCustomerPoolConfigService customerPoolConfigService;
@Resource
private CrmBusinessService businessService;  // 新增
```

**涉及文件**：
- `Server/mitedtsm-module-crm/src/main/java/com/meession/etm/module/crm/controller/admin/customer/CrmCustomerController.java`

---

## 3. 中级Bug

| Bug ID | 标题 | 状态 | 功能模块 |
| :---: | :--- | :---: | :---: |
| BUG-012 | 前端CustomerVO接口缺少商机统计字段 | ✅ 已修复 | 客户-商机集成 |
| BUG-013 | CustomerDetailsHeader缺少金额格式化工具 | ✅ 已修复 | 客户-商机集成 |
| BUG-014 | 阿拉伯语国际化缺失新增Key | ✅ 已修复 | 客户-商机集成 |

### BUG-012: 前端CustomerVO接口缺少商机统计字段

**问题描述**：前端`CustomerVO`接口缺少`businessCount`和`totalDealAmount`字段，导致无法接收后端返回的商机统计数据。

**影响范围**：客户详情页商机统计信息展示

**错误代码**：

```typescript
// 修复前（缺少字段）
export interface CustomerVO {
  id: number
  name: string
  // ... 其他字段 ...
  // 缺少 businessCount 和 totalDealAmount
}
```

**修复方案**：

```typescript
// 修复后（新增字段）
export interface CustomerVO {
  id: number
  name: string
  // ... 其他字段 ...
  businessCount?: number // 商机数量
  totalDealAmount?: number // 成交总金额
}
```

**涉及文件**：
- `Web/src/api/crm/customer/index.ts`

---

### BUG-013: CustomerDetailsHeader缺少金额格式化工具

**问题描述**：`CustomerDetailsHeader.vue`中展示成交总金额时缺少金额格式化工具导入，导致金额显示为原始数字而非格式化后的金额。

**影响范围**：客户详情页成交金额显示

**错误代码**：

```typescript
// 修复前（缺少导入）
import { formatDate } from '@/utils/formatTime'
// 缺少 formatPriceWithCurrency 导入
```

**修复方案**：

```typescript
// 修复后（新增导入）
import { formatDate } from '@/utils/formatTime'
import { formatPriceWithCurrency } from '@/utils/formatter'
```

**涉及文件**：
- `Web/src/views/crm/customer/detail/CustomerDetailsHeader.vue`

---

### BUG-014: 阿拉伯语国际化缺失新增Key

**问题描述**：`ar/crm.ts`中缺少客户商机集成相关的国际化Key，导致阿拉伯语环境下页面显示key名称而非翻译文本。

**影响范围**：阿拉伯语环境下的客户详情页统计Tab

**缺失的Key**：
- `statisticsTab` - 统计分析
- `businessStatusDistribution` - 商机状态分布
- `businessAmountDistribution` - 商机金额分布
- `customerLevelDistribution` - 客户等级分布
- `customerSourceDistribution` - 客户来源分布
- `customerIndustryDistribution` - 客户行业分布
- `customerStatusDistribution` - 客户状态分布
- `businessStatus` - 商机状态
- `businessAmount` - 商机金额

**修复方案**：在`ar/crm.ts`中补充上述key的阿拉伯语翻译

**涉及文件**：
- `Web/src/locales/ar/crm.ts`

---

## 4. 已解决的历史Bug

| Bug ID | 标题 | 解决日期 | 解决方式 |
| :---: | :--- | :---: | :--- |
| BUG-H03 | 数据库连接问题 | 2026-07-18 | 通过`docker ps`验证MySQL（3306端口）和Redis（6379端口）容器是否正常运行 |

---

## 5. 潜在问题与优化建议

| 建议ID | 标题 | 优先级 | 描述 | 功能模块 |
| :---: | :--- | :---: | :--- | :---: |
| SUG-006 | 商机统计查询性能优化 | 中 | `buildCustomerDetailList`方法中循环调用`getBusinessCountByCustomerId`和`getTotalDealAmountByCustomerId`，建议使用批量查询优化性能 | 客户-商机集成 |
| SUG-007 | 客户统计组件数据缓存 | 中 | `CustomerStatistics.vue`每次切换Tab都重新加载数据，建议增加数据缓存机制 | 客户-商机集成 |

---

## Bug统计

| 状态 | 数量 |
| :--- | :---: |
| 已修复 | 7 |
| 待修复 | 0 |
| 总计 | 7 |

| 严重程度 | 数量 |
| :--- | :---: |
| 严重级 | 2 |
| 高级 | 2 |
| 中级 | 3 |
| 总计 | 7 |

| 功能模块 | 数量 |
| :--- | :---: |
| 环境配置 | 2 |
| 客户-商机集成 | 5 |
| 总计 | 7 |

---

## 变更记录

| 日期 | 版本 | 变更内容 | 作者 |
| :---: | :---: | :--- | :---: |
| 2026-07-18 | v1.0 | 初始版本，记录7个已修复的Bug（客户导入导出、星级评估、客户商机集成相关） | 开发团队 |
