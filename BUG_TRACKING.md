# Bug追踪文档

## 文档信息

| 项目 | 说明 |
| :--- | :--- |
| **项目名称** | Group-11 CRM系统 |
| **文档版本** | v1.0 |
| **创建日期** | 2026-07-17 |
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

## 2. 高级Bug

| Bug ID | 标题 | 状态 | 发现日期 | 修复日期 |
| :---: | :--- | :---: | :---: | :---: |
| BUG-003 | API模块导入方式错误 | ✅ 已修复 | 2026-07-17 | 2026-07-17 |
| BUG-004 | 图表字段映射错误 | ✅ 已修复 | 2026-07-17 | 2026-07-17 |

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

## 3. 中级Bug

| Bug ID | 标题 | 状态 | 发现日期 | 修复日期 |
| :---: | :--- | :---: | :---: | :---: |
| BUG-005 | 配置分页查询错误过滤禁用状态 | ✅ 已修复 | 2026-07-17 | 2026-07-17 |
| BUG-006 | 未使用的导入 | ✅ 已修复 | 2026-07-17 | 2026-07-17 |
| BUG-007 | 缺失国际化Key | ✅ 已修复 | 2026-07-17 | 2026-07-17 |

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

## 4. 已解决的历史Bug

| Bug ID | 标题 | 解决日期 | 解决方式 |
| :---: | :--- | :---: | :--- |
| BUG-H01 | 国际化文件路径查找失败 | 2026-07-17 | 通过`LS`命令查看目录结构，定位到正确路径 |
| BUG-H02 | 客户状态分析Tab组件路径错误 | 2026-07-17 | 通过`LS`命令发现统计组件位于`portrait/components`目录下，在正确位置创建组件 |

---

## 5. 潜在问题与优化建议

| 建议ID | 标题 | 优先级 | 描述 |
| :---: | :--- | :---: | :--- |
| SUG-001 | 实现真实分页 | 中 | `getConfigPage()`方法返回全量数据，未使用分页参数。建议使用`BaseMapperX.selectPage()`实现真实分页 |
| SUG-002 | 增加空值检查 | 中 | `getUserIds()`方法中当`deptId`为null时，`deptApi.getChildDeptList(null)`行为不确定。建议增加空值检查，返回当前用户ID |
| SUG-003 | 移除未使用的参数 | 低 | `CustomerStatistics.vue`组件接收了`customerId`属性但未在API调用中使用。建议移除未使用的prop或在API调用中传递 |
| SUG-004 | API模块命名规范 | 低 | `StatisticsPortraitApi`作为命名空间导出，建议统一使用默认导出或更明确的命名方式 |
| SUG-005 | 国际化key命名规范 | 低 | 建议使用统一的命名规范，如`xxxTab`用于Tab标签，`xxxDistribution`用于分布类图表标题 |

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

---

## 变更记录

| 日期 | 版本 | 变更内容 | 作者 |
| :---: | :---: | :--- | :---: |
| 2026-07-17 | v1.0 | 初始版本，记录7个已修复的Bug | 开发团队 |