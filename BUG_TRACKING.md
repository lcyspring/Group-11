# Bug追踪文档

## 文档信息

| 项目 | 说明 |
| :--- | :--- |
| **项目名称** | Group-11 CRM系统 |
| **文档版本** | v2.0 |
| **创建日期** | 2026-07-17 |
| **更新日期** | 2026-07-18 |
| **作者** | 开发团队 |
| **状态** | 活跃 |

---

## 目录

1. [严重级Bug](#1-严重级bug)
2. [高级Bug](#2-高级bug)
3. [中级Bug](#3-中级bug)
4. [已解决的历史Bug](#4-已解决的历史bug)
5. [潜在问题与优化建议](#5-潜在问题与优化建议)

---

## 1. 严重级Bug

| Bug ID | 标题 | 状态 | 发现日期 | 修复日期 |
| :---: | :--- | :---: | :---: | :---: |
| BUG-001 | SQL DDL列名与DO不一致 | ✅ 已修复 | 2026-07-17 | 2026-07-17 |
| BUG-002 | Controller缺少必要的import语句 | ✅ 已修复 | 2026-07-17 | 2026-07-17 |
| BUG-008 | Vite启动失败，pnpm dev命令无法启动 | ✅ 已修复 | 2026-07-18 | 2026-07-18 |
| BUG-009 | Maven插件错误，spring-boot:run命令无法执行 | ✅ 已修复 | 2026-07-18 | 2026-07-18 |

### BUG-001: SQL DDL列名与DO不一致

**问题描述**：`crm_customer_config`表的DDL脚本使用了错误的列名，与`CrmCustomerConfigDO`实体类字段不匹配。

**影响范围**：客户配置模块的所有API（创建/更新/查询配置）

**错误代码**：

```sql
-- 修复前（错误）
`type` VARCHAR(50) NOT NULL COMMENT '配置类型',
`name` VARCHAR(100) NOT NULL COMMENT '配置名称',
`value` VARCHAR(500) NULL COMMENT '配置值',
```

**修复方案**：

```sql
-- 修复后（正确）
`config_type` VARCHAR(50) NOT NULL COMMENT '配置类型',
`config_value` INT(11) NULL COMMENT '配置值',
`config_name` VARCHAR(100) NOT NULL COMMENT '配置名称',
`color` VARCHAR(50) NULL COMMENT '颜色',
```

**涉及文件**：
- `Server/sql/mysql/crm-customer-status.sql`

---

### BUG-002: Controller缺少必要的import语句

**问题描述**：`CrmStatisticsPortraitController`缺少`CrmStatisticCustomerStatusRespVO`的import语句，导致编译失败。

**影响范围**：客户状态统计接口

**错误代码**：

```java
// 缺少以下import
import com.meession.etm.module.crm.controller.admin.statistics.vo.portrait.CrmStatisticCustomerStatusRespVO;
```

**修复方案**：添加缺失的import语句

**涉及文件**：
- `Server/mitedtsm-module-crm/src/main/java/com/meession/etm/module/crm/controller/admin/statistics/CrmStatisticsPortraitController.java`

---

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

| Bug ID | 标题 | 状态 | 发现日期 | 修复日期 |
| :---: | :--- | :---: | :---: | :---: |
| BUG-003 | API模块导入方式错误 | ✅ 已修复 | 2026-07-17 | 2026-07-17 |
| BUG-004 | 图表字段映射错误 | ✅ 已修复 | 2026-07-17 | 2026-07-17 |
| BUG-010 | 客户详情VO缺少商机统计字段 | ✅ 已修复 | 2026-07-18 | 2026-07-18 |
| BUG-011 | CustomerController未注入商机服务 | ✅ 已修复 | 2026-07-18 | 2026-07-18 |

### BUG-003: API模块导入方式错误

**问题描述**：`CustomerStatistics.vue`中使用`import * as PortraitApi`导入API模块，但实际导出的是命名空间`StatisticsPortraitApi`，导致调用失败。

**影响范围**：客户详情统计分析Tab组件

**错误代码**：

```typescript
// 修复前（错误）
import * as PortraitApi from '@/api/crm/statistics/portrait'
PortraitApi.getCustomerStatus({}) // 错误！不存在该方法
```

**修复方案**：

```typescript
// 修复后（正确）
import { StatisticsPortraitApi } from '@/api/crm/statistics/portrait'
StatisticsPortraitApi.getCustomerStatus({}) // 正确
```

**涉及文件**：
- `Web/src/views/crm/customer/detail/CustomerStatistics.vue`

---

### BUG-004: 图表字段映射错误

**问题描述**：行业分布图表使用`'industry'`作为labelField，但API返回的字段名为`industryId`，导致图表无法正确显示数据。

**影响范围**：客户详情统计分析Tab组件的行业分布图表

**错误代码**：

```typescript
// 修复前（错误）
const industryChartOption = computed(() => getChartOption(industryList.value, 'industry', 'customerCount', t('industryId')))
```

**修复方案**：

```typescript
// 修复后（正确）
const industryChartOption = computed(() => getChartOption(industryList.value, 'industryId', 'customerCount', t('industryId')))
```

**涉及文件**：
- `Web/src/views/crm/customer/detail/CustomerStatistics.vue`

---

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

| Bug ID | 标题 | 状态 | 发现日期 | 修复日期 |
| :---: | :--- | :---: | :---: | :---: |
| BUG-005 | 配置分页查询错误过滤禁用状态 | ✅ 已修复 | 2026-07-17 | 2026-07-17 |
| BUG-006 | 未使用的导入 | ✅ 已修复 | 2026-07-17 | 2026-07-17 |
| BUG-007 | 缺失国际化Key | ✅ 已修复 | 2026-07-17 | 2026-07-17 |
| BUG-012 | 前端CustomerVO接口缺少商机统计字段 | ✅ 已修复 | 2026-07-18 | 2026-07-18 |
| BUG-013 | CustomerDetailsHeader缺少金额格式化工具 | ✅ 已修复 | 2026-07-18 | 2026-07-18 |

### BUG-005: 配置分页查询错误过滤禁用状态

**问题描述**：`getConfigPage()`方法使用`selectListByConfigType()`查询，该方法会过滤掉`status=false`的禁用配置，导致分页列表无法显示所有配置记录。

**影响范围**：客户配置分页查询接口

**错误代码**：

```java
// 修复前（错误）
@Override
public PageResult<CrmCustomerConfigRespVO> getConfigPage(String configType) {
    List<CrmCustomerConfigDO> list = configMapper.selectListByConfigType(configType); // 过滤了禁用状态
    return new PageResult<>(voList, (long) voList.size());
}
```

**修复方案**：

```java
// 修复后（正确）
@Override
public PageResult<CrmCustomerConfigRespVO> getConfigPage(String configType) {
    List<CrmCustomerConfigDO> list = configMapper.selectListByConfigTypeIncludeDisabled(configType); // 不过滤禁用状态
    return new PageResult<>(voList, (long) voList.size());
}
```

**涉及文件**：
- `Server/mitedtsm-module-crm/src/main/java/com/meession/etm/module/crm/service/customer/CrmCustomerConfigServiceImpl.java`
- `Server/mitedtsm-module-crm/src/main/java/com/meession/etm/module/crm/dal/mysql/customer/CrmCustomerConfigMapper.java`

---

### BUG-006: 未使用的导入

**问题描述**：`CustomerStatistics.vue`导入了`DICT_TYPE`但未使用，造成代码冗余。

**影响范围**：代码质量

**错误代码**：

```typescript
import { DICT_TYPE } from '@/utils/dict' // 未使用
```

**修复方案**：移除未使用的导入

**涉及文件**：
- `Web/src/views/crm/customer/detail/CustomerStatistics.vue`

---

### BUG-007: 缺失国际化Key

**问题描述**：客户详情统计分析Tab组件中使用了未定义的国际化key，导致页面显示key名称而非翻译文本。

**影响范围**：客户详情统计分析Tab组件的图表标题

**缺失的Key**：
- `statistics` - 状态分布
- `levelDistribution` - 等级分布
- `sourceDistribution` - 来源分布
- `industryDistribution` - 行业分布

**修复方案**：在中文和英文语言文件中补充上述key的翻译

**涉及文件**：
- `Web/src/locales/zh-CN/crm.ts`
- `Web/src/locales/en/crm.ts`

---

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
| BUG-H01 | 国际化文件路径查找失败 | 2026-07-17 | 通过`LS`命令查看目录结构，定位到正确路径 |
| BUG-H02 | 客户状态分析Tab组件路径错误 | 2026-07-17 | 通过`LS`命令发现统计组件位于`portrait/components`目录下，在正确位置创建组件 |
| BUG-H03 | 数据库连接问题 | 2026-07-18 | 通过`docker ps`验证MySQL（3306端口）和Redis（6379端口）容器是否正常运行 |

---

## 5. 潜在问题与优化建议

| 建议ID | 标题 | 优先级 | 描述 |
| :---: | :--- | :---: | :--- |
| SUG-001 | 实现真实分页 | 中 | `getConfigPage()`方法返回全量数据，未使用分页参数。建议使用`BaseMapperX.selectPage()`实现真实分页 |
| SUG-002 | 增加空值检查 | 中 | `getUserIds()`方法中当`deptId`为null时，`deptApi.getChildDeptList(null)`行为不确定。建议增加空值检查，返回当前用户ID |
| SUG-003 | 移除未使用的参数 | 低 | `CustomerStatistics.vue`组件接收了`customerId`属性但未在API调用中使用。建议移除未使用的prop或在API调用中传递 |
| SUG-004 | API模块命名规范 | 低 | `StatisticsPortraitApi`作为命名空间导出，建议统一使用默认导出或更明确的命名方式 |
| SUG-005 | 国际化key命名规范 | 低 | 建议使用统一的命名规范，如`xxxTab`用于Tab标签，`xxxDistribution`用于分布类图表标题 |
| SUG-006 | 商机统计查询性能优化 | 中 | `buildCustomerDetailList`方法中循环调用`getBusinessCountByCustomerId`和`getTotalDealAmountByCustomerId`，建议使用批量查询优化性能 |
| SUG-007 | 客户统计组件数据缓存 | 中 | `CustomerStatistics.vue`每次切换Tab都重新加载数据，建议增加数据缓存机制 |

---

## Bug统计

| 状态 | 数量 |
| :--- | :---: |
| 已修复 | 14 |
| 待修复 | 0 |
| 总计 | 14 |

| 严重程度 | 数量 |
| :--- | :---: |
| 严重级 | 4 |
| 高级 | 4 |
| 中级 | 6 |
| 总计 | 14 |

---

## 变更记录

| 日期 | 版本 | 变更内容 | 作者 |
| :---: | :---: | :--- | :---: |
| 2026-07-17 | v1.0 | 初始版本，记录7个已修复的Bug | 开发团队 |
| 2026-07-18 | v2.0 | 新增7个Bug记录（客户导入导出、星级评估、客户商机集成相关），更新统计数据 | 开发团队 |
