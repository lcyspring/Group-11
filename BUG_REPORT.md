# Bug发现与汇总报告

## 报告信息

| 项目 | 内容 |
|------|------|
| 报告标题 | CRM客户模块开发 - Bug发现与汇总报告 |
| 报告日期 | 2026-07-14 |
| 开发功能 | 1.客户模块数据库表结构与公海机制规则引擎 2.客户CRUD接口与客户查重服务 |
| 报告作者 | 系统自动生成 |

---

## 一、已解决的Bug

### 1.1 编译错误 - 缺少CrmCustomerPoolLogPageReqVO类

**问题描述：**
编译时提示 `程序包com.meession.etm.module.crm.controller.admin.customer.vo.pool不存在`，缺少公海操作日志分页查询VO类。

**涉及文件：**
- `mitedtsm-module-crm/src/main/java/com/meession/etm/module/crm/controller/admin/customer/vo/pool/CrmCustomerPoolLogPageReqVO.java`（新建）

**修复方案：**
创建 `CrmCustomerPoolLogPageReqVO.java` 类，定义分页查询参数。

**修复状态：** ✅ 已修复

---

### 1.2 编译错误 - LambdaUpdateWrapperX不存在

**问题描述：**
编译时提示 `找不到符号 LambdaUpdateWrapperX`，项目使用的是MyBatis-Plus原生API。

**涉及文件：**
- `mitedtsm-module-crm/src/main/java/com/meession/etm/module/crm/dal/mysql/customer/CrmCustomerMapper.java`

**修复方案：**
将 `LambdaUpdateWrapperX` 替换为MyBatis-Plus原生的 `LambdaUpdateWrapper`，并调整import语句。

**修复状态：** ✅ 已修复

---

### 1.3 编译错误 - betweenIfPresent参数类型不兼容

**问题描述：**
编译时提示 `不兼容的类型: java.time.LocalDateTime无法转换为java.lang.Object[]`，时间范围查询参数类型不匹配。

**涉及文件：**
- `mitedtsm-module-crm/src/main/java/com/meession/etm/module/crm/controller/admin/customer/vo/pool/CrmCustomerPoolLogPageReqVO.java`

**修复方案：**
修改 `createTime` 字段为 `LocalDateTime[]` 数组类型，以支持时间范围查询。

**修复状态：** ✅ 已修复

---

### 1.4 前端验证码组件未导入

**问题描述：**
前端登录页面使用 `<Verify>` 组件但未导入，导致Vite编译错误。

**涉及文件：**
- `Web/src/views/Login/components/LoginForm.vue`

**修复方案：**
在LoginForm.vue中添加 `import { Verify } from '@/components/Verifition'`。

**修复状态：** ✅ 已修复

---

### 1.5 前端环境变量未定义

**问题描述：**
Vite使用 `--mode env.local` 启动，但缺少 `.env.env.local` 文件，导致 `VITE_BASE_URL` 和 `VITE_API_URL` 未定义，API请求失败。

**涉及文件：**
- `Web/.env.env.local`（新建）

**修复方案：**
创建 `.env.env.local` 文件，添加必要的环境变量配置。

**修复状态：** ✅ 已修复

---

### 1.6 CrmCustomerMapper中缺少Collections导入

**问题描述：**
新增的 `selectDuplicateCustomers` 方法中使用了 `Collections.emptyList()`，但未导入 `java.util.Collections`。

**涉及文件：**
- `mitedtsm-module-crm/src/main/java/com/meession/etm/module/crm/dal/mysql/customer/CrmCustomerMapper.java`

**修复方案：**
在文件中添加 `import java.util.Collections;`。

**修复状态：** ✅ 已修复

---

## 二、待修复的Bug

### 2.1 PoolReceiveRule领取数量限制方法未实现

**问题描述：**
`PoolReceiveRule` 类中的 `getDailyReceiveCount`、`getWeeklyReceiveCount`、`getMonthlyReceiveCount` 方法返回硬编码的0，没有实际查询数据库统计领取数量。

**严重程度：** 🔴 高

**涉及文件：**
- `mitedtsm-module-crm/src/main/java/com/meession/etm/module/crm/service/customer/rule/PoolReceiveRule.java:43-53`

**代码问题：**
```java
private long getDailyReceiveCount(Long userId) {
    return 0;  // 硬编码返回0，未实现实际逻辑
}

private long getWeeklyReceiveCount(Long userId) {
    return 0;  // 硬编码返回0，未实现实际逻辑
}

private long getMonthlyReceiveCount(Long userId) {
    return 0;  // 硬编码返回0，未实现实际逻辑
}
```

**影响：**
公海领取数量限制规则无法生效，用户可以无限制领取公海客户。

**修复建议：**
- 创建公海领取记录Mapper方法，按时间范围统计用户领取数量
- 实现按日、周、月的领取数量统计逻辑

---

### 2.2 PoolDistributeRule分配规则未实现

**问题描述：**
`PoolDistributeRule` 类中的 `execute` 方法只打印日志，没有实际实现客户分配逻辑。

**严重程度：** 🔴 高

**涉及文件：**
- `mitedtsm-module-crm/src/main/java/com/meession/etm/module/crm/service/customer/rule/PoolDistributeRule.java:19-21`

**代码问题：**
```java
@Override
public void execute(CrmCustomerPoolRuleDO rule) {
    log.info("开始执行公海分配规则: {}", rule.getName());
    CrmCustomerPoolDistributeRuleConfig config = parseConfig(rule, CrmCustomerPoolDistributeRuleConfig.class);
    // 缺少实际分配逻辑
}
```

**影响：**
公海自动分配规则无法执行，客户无法自动分配给销售人员。

**修复建议：**
- 实现从公海筛选符合条件的客户
- 根据配置的分配策略（轮询、随机、按能力等）分配客户给指定用户
- 更新客户负责人信息

---

### 2.3 PoolRuleEngine规则映射缓存并发问题

**问题描述：**
`PoolRuleEngine` 类中的 `ruleMap` 使用懒加载初始化，但未考虑并发场景下的线程安全问题。

**严重程度：** 🟡 中

**涉及文件：**
- `mitedtsm-module-crm/src/main/java/com/meession/etm/module/crm/service/customer/rule/PoolRuleEngine.java:27-33`

**代码问题：**
```java
private Map<Integer, IPoolRule> getRuleMap() {
    if (ruleMap == null) {
        ruleMap = rules.stream()
                .collect(Collectors.toMap(IPoolRule::getRuleType, Function.identity()));
    }
    return ruleMap;
}
```

**影响：**
在高并发场景下可能导致重复初始化或Map数据不一致。

**修复建议：**
- 使用 `synchronized` 或 `Double-Checked Locking` 保证线程安全
- 或在 `@PostConstruct` 方法中初始化

---

### 2.4 客户查重未排除当前客户

**问题描述：**
`CrmCustomerServiceImpl.checkDuplicate` 方法在查重时没有排除当前客户（更新场景），导致更新客户信息时会误判为重复。

**严重程度：** 🟡 中

**涉及文件：**
- `mitedtsm-module-crm/src/main/java/com/meession/etm/module/crm/service/customer/CrmCustomerServiceImpl.java:658-694`

**影响：**
更新客户信息时，如果只修改非查重字段（如地址、备注），会错误地提示存在重复客户。

**修复建议：**
- 在 `CrmCustomerDuplicateCheckReqVO` 中添加 `excludeId` 字段
- 在查询时排除指定ID的客户

---

### 2.5 客户查重SQL LIMIT方言兼容性

**问题描述：**
`CrmCustomerMapper.selectDuplicateCustomers` 方法使用 `query.last("LIMIT 20")` 限制查询结果，这在MySQL中有效，但在其他数据库（如Oracle、PostgreSQL）中语法不同。

**严重程度：** 🟡 中

**涉及文件：**
- `mitedtsm-module-crm/src/main/java/com/meession/etm/module/crm/dal/mysql/customer/CrmCustomerMapper.java:291`

**代码问题：**
```java
query.last("LIMIT 20");
```

**影响：**
如果项目需要迁移到其他数据库，该查询会失败。

**修复建议：**
- 使用MyBatis-Plus的分页插件替代 `last()` 方法
- 或使用数据库方言适配

---

### 2.6 匹配度计算可能超过100分

**问题描述：**
`calculateMatchScore` 方法中各字段匹配分数累加可能超过100分，不符合设计预期（匹配度应为0-100）。

**严重程度：** 🟢 低

**涉及文件：**
- `mitedtsm-module-crm/src/main/java/com/meession/etm/module/crm/service/customer/CrmCustomerServiceImpl.java:696-746`

**代码问题：**
```java
// 当前评分规则：name(40) + mobile(30) + telephone(30) + email(25) + qq(20) + wechat(20) = 165
if (customer.getName().equalsIgnoreCase(reqVO.getName())) {
    score += 40;  // 可能超过100
}
```

**影响：**
匹配度分数超过100，不符合语义预期，前端展示可能需要额外处理。

**修复建议：**
- 在方法末尾添加 `score = Math.min(score, 100)` 限制分数上限
- 或调整各字段权重，使总分不超过100

---

## 三、潜在风险点

### 3.1 规则引擎缺少规则注册验证

**风险描述：**
`PoolRuleEngine` 在初始化时没有验证所有规则类型是否都有对应的实现类，如果数据库中存在未注册的规则类型，只会打印警告日志而不会抛出异常。

**风险等级：** 🟡 中

**建议：**
在启动时校验所有规则类型是否都有对应的实现类，确保系统完整性。

---

### 3.2 公海规则执行缺少事务管理

**风险描述：**
`PoolRecycleRule.execute` 方法中循环调用 `customerService.putCustomerPool`，如果中间某个客户处理失败，前面的客户已经被修改，但没有回滚机制。

**风险等级：** 🟡 中

**建议：**
考虑添加批量事务处理或补偿机制，确保规则执行的原子性。

---

### 3.3 客户查重性能问题

**风险描述：**
`selectDuplicateCustomers` 方法在模糊匹配模式下使用 `like` 查询客户名称，如果客户表数据量大，可能导致性能问题。

**风险等级：** 🟢 低（当前阶段数据量较小）

**建议：**
- 添加索引优化模糊查询
- 考虑使用全文搜索引擎（如Elasticsearch）处理复杂查重场景

---

## 四、Bug统计

| 类别 | 数量 | 已修复 | 待修复 |
|------|------|--------|--------|
| 编译错误 | 6 | 6 | 0 |
| 功能缺陷 | 4 | 0 | 4 |
| 代码规范 | 2 | 0 | 2 |
| 潜在风险 | 3 | 0 | 3 |
| **合计** | **15** | **6** | **9** |

---

## 五、优先级排序

| 优先级 | Bug编号 | Bug描述 | 预计修复时间 |
|--------|---------|---------|--------------|
| P0 | 2.1 | PoolReceiveRule领取数量限制方法未实现 | 1天 |
| P0 | 2.2 | PoolDistributeRule分配规则未实现 | 2天 |
| P1 | 2.4 | 客户查重未排除当前客户 | 0.5天 |
| P1 | 2.3 | PoolRuleEngine规则映射缓存并发问题 | 0.5天 |
| P2 | 2.5 | 客户查重SQL LIMIT方言兼容性 | 1天 |
| P2 | 2.6 | 匹配度计算可能超过100分 | 0.5天 |
| P3 | 3.1 | 规则引擎缺少规则注册验证 | 0.5天 |
| P3 | 3.2 | 公海规则执行缺少事务管理 | 1天 |
| P3 | 3.3 | 客户查重性能问题 | 2天 |

---

## 六、总结

本次开发共发现 **15** 个问题，其中 **6** 个编译错误已全部修复，**9** 个问题待后续修复。

**核心问题：**
1. 公海规则引擎的领取规则和分配规则尚未完全实现，需要优先完成
2. 客户查重功能缺少排除当前客户的逻辑，可能影响更新操作

**建议修复顺序：**
1. 优先修复 P0 级别的规则引擎功能缺陷
2. 其次修复 P1 级别的数据一致性问题
3. 最后处理 P2/P3 级别的代码优化和潜在风险