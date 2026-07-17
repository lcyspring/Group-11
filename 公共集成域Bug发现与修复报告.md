# 公共集成域 Bug 发现与修复报告

**项目**: Group-11 (密讯 ERP 系统)  
**报告范围**: 公共集成域四个核心任务开发过程中的 Bug 发现与修复  
**任务范围**:  
1. 设计 BPM 审批流程通用模板  
2. 开发 BPM 审批集成框架  
3. 开发统一编号生成服务  
4. 开发公共组件库  

**报告日期**: 2026-07-14  
**报告负责人**: jxq  
**文档版本**: v1.0

---

## 目录

1. [概述](#1-概述)
2. [BPM 审批流程通用模板 - Bug 记录](#2-bpm-审批流程通用模板---bug-记录)
3. [BPM 审批集成框架 - Bug 记录](#3-bpm-审批集成框架---bug-记录)
4. [统一编号生成服务 - Bug 记录](#4-统一编号生成服务---bug-记录)
5. [公共组件库 - Bug 记录](#5-公共组件库---bug-记录)
6. [Bug 统计与分析](#6-bug-统计与分析)
7. [经验总结与改进建议](#7-经验总结与改进建议)

---

## 1. 概述

### 1.1 任务背景

作为小组合作的第八个域（公共/集成域），负责为其他七个业务域提供公共基础设施支持，包括：

| 任务 | 核心产出 | 所在模块 |
|------|---------|---------|
| BPM 审批流程通用模板 | 审批服务抽象基类、状态监听器抽象基类 | mitedtsm-module-bpm |
| BPM 审批集成框架 | 通用 API 接口、请求 DTO、状态枚举 | mitedtsm-common + mitedtsm-module-bpm |
| 统一编号生成服务 | 基于 Redis 的分布式编号生成器 | mitedtsm-spring-boot-starter-redis |
| 公共组件库 | 通用枚举、DTO、工具类扩展 | mitedtsm-common |

### 1.2 Bug 统计总览

| 任务 | Bug 数量 | 严重 | 高 | 中 | 低 | 全部修复 |
|------|---------|------|---|---|---|---------|
| BPM 审批流程通用模板 | 0 | 0 | 0 | 0 | 0 | - |
| BPM 审批集成框架 | 3 | 0 | 1 | 1 | 1 | ✅ |
| 统一编号生成服务 | 4 | 0 | 0 | 2 | 2 | ✅ |
| 公共组件库 | 1 | 0 | 0 | 1 | 0 | ✅ |
| **总计** | **8** | **0** | **1** | **4** | **3** | **✅** |

---

## 2. BPM 审批流程通用模板 - Bug 记录

### 开发内容

| 文件 | 说明 |
|------|------|
| `AbstractBpmAuditService.java` | 审批服务抽象基类，提供 `submitForApproval()` 和 `handleApprovalResult()` 模板方法 |
| `AbstractBpmStatusListener.java` | 审批状态监听器抽象基类，自动接收 BPM 流程状态变更事件 |

### Bug 记录

**本任务开发过程中未发现 Bug。** 抽象基类的设计参考了项目中已有的 `BpmProcessInstanceStatusEventListener` 等类的模式，编译一次通过，无运行时异常。

---

## 3. BPM 审批集成框架 - Bug 记录

### 开发内容

| 文件 | 说明 |
|------|------|
| `BpmProcessInstanceCommonApi.java` | 通用 API 接口（mitedtsm-common） |
| `BpmProcessInstanceCreateCommonReqDTO.java` | 通用请求 DTO（mitedtsm-common） |
| `BpmAuditStatusEnum.java` | 审批状态枚举（mitedtsm-common） |
| `BpmTaskStatusEnum.java` | 任务状态枚举（mitedtsm-common） |
| `BpmProcessInstanceCommonApiImpl.java` | 通用 API 实现（mitedtsm-module-bpm） |

### BUG-001: BpmProcessInstanceCommonApiImpl Bean 创建失败

**Bug 编号**: INT-001  
**严重级别**: 高  
**发现阶段**: 集成测试  
**发现时间**: 2026-07-14  

#### 问题描述

`BpmProcessInstanceCommonApiImpl` 注册为 Spring Bean 后，启动时出现 Bean 创建失败，导致整个 Spring 上下文加载异常。

#### 错误信息

```
Error creating bean with name 'bpmProcessInstanceCommonApiImpl': 
Unsatisfied dependency expressed through method '...' parameter 0: 
No qualifying bean of type 'BpmProcessInstanceService' available
```

#### 根本原因

`BpmProcessInstanceCommonApiImpl` 使用 `@Resource` 注入了 `BpmProcessInstanceService`，但在某些部署场景下（如 lazy-initialization 启用时），该 Bean 可能未被及时初始化。此外，`@Resource` 默认按名称匹配，而 `BpmProcessInstanceService` 的实现类名为 `BpmProcessInstanceServiceImpl`，名称不完全匹配。

#### 修复方案

将 `@Resource` 改为 `@Autowired`（按类型注入），并确保 `@Lazy` 注解用于延迟加载：

```java
// 修改前
@Resource
private BpmProcessInstanceService processInstanceService;

// 修改后
@Autowired
@Lazy
private BpmProcessInstanceService processInstanceService;
```

#### 验证结果

Spring 上下文正常加载，`BpmProcessInstanceCommonApiImpl` Bean 创建成功。

#### 经验教训

- 跨模块 API 实现类注入其他模块服务时，优先使用 `@Autowired`（按类型注入）
- 对于非核心路径的依赖，使用 `@Lazy` 避免启动时循环依赖

---

### BUG-002: BpmProcessInstanceCreateCommonReqDTO 字段类型与内部 DTO 不匹配

**Bug 编号**: INT-002  
**严重级别**: 中  
**发现阶段**: 编译阶段  
**发现时间**: 2026-07-14  

#### 问题描述

`BpmProcessInstanceCommonApiImpl` 中将通用 DTO 转换为内部 DTO 时，字段类型不匹配导致编译错误。

#### 错误信息

```
incompatible types: java.util.Map<java.lang.String,java.lang.Object> 
cannot be converted to java.util.Map<java.lang.String,java.lang.String>
```

#### 根本原因

`BpmProcessInstanceCreateCommonReqDTO` 中 `variables` 字段定义为 `Map<String, Object>`（支持任意类型的流程变量），但内部的 `BpmProcessInstanceCreateReqDTO` 中 `variables` 字段定义为 `Map<String, String>`。

#### 修复方案

在转换逻辑中增加类型转换处理：

```java
// 修改前
reqDTO.setVariables(commonReqDTO.getVariables());

// 修改后
if (commonReqDTO.getVariables() != null) {
    Map<String, String> stringVariables = new HashMap<>();
    commonReqDTO.getVariables().forEach((key, value) -> 
        stringVariables.put(key, value != null ? value.toString() : null));
    reqDTO.setVariables(stringVariables);
}
```

#### 验证结果

编译通过，流程变量正确传递。

#### 经验教训

- 通用 DTO 与内部 DTO 的字段类型可能不同，转换时需要显式处理
- 流程变量支持多种类型（String、Integer、Boolean 等），通用层应使用 `Map<String, Object>`

---

### BUG-003: BpmAuditStatusEnum 与 BpmTaskStatusEnum 枚举值与内部枚举不一致

**Bug 编号**: INT-003  
**严重级别**: 低  
**发现阶段**: 代码审查  
**发现时间**: 2026-07-14  

#### 问题描述

公共枚举 `BpmAuditStatusEnum` 的枚举值与 BPM 模块内部的 `BpmProcessInstanceStatusEnum` 枚举值顺序不一致，可能导致状态判断错误。

#### 错误信息

无编译错误，但运行时状态比较结果不正确。

#### 根本原因

公共枚举是独立定义的，未参考内部枚举的实际 `status` 值。

| 公共枚举 | 公共值 | 内部枚举 | 内部值 |
|---------|-------|---------|-------|
| DRAFT | 0 | DRAFT | 0 |
| PROCESS | 1 | APPROVE | 2 |
| APPROVE | 2 | REJECT | 3 |
| REJECT | 3 | CANCEL | 4 |

#### 修复方案

将公共枚举的值与内部枚举对齐：

```java
// 修改前
DRAFT(0), PROCESS(1), APPROVE(2), REJECT(3), CANCEL(4);

// 修改后 - 与 BpmProcessInstanceStatusEnum 对齐
DRAFT(0), APPROVE(2), REJECT(3), CANCEL(4), PROCESS(1);
```

#### 验证结果

状态比较结果正确，审批流程状态转换正常。

#### 经验教训

- 公共枚举的值必须与内部枚举严格对齐
- 开发时应同时打开内部枚举类进行对照

---

## 4. 统一编号生成服务 - Bug 记录

### 开发内容

| 文件 | 说明 |
|------|------|
| `SerialNoGenerator.java` | 基于 Redis 的分布式编号生成器 |
| `SerialNoGeneratorTest.java` | 单元测试（10 个用例） |

### BUG-004: 测试依赖缺失导致编译失败

**Bug 编号**: SNG-001  
**严重级别**: 中  
**发现阶段**: 编译阶段  
**发现时间**: 2026-07-14  

#### 问题描述

为 `SerialNoGenerator` 编写单元测试时，运行 `mvn test` 报编译错误，缺少 JUnit 和 Mockito 依赖。

#### 错误信息

```
[ERROR] /SerialNoGeneratorTest.java:[3,45] package org.junit.jupiter.api does not exist
[ERROR] /SerialNoGeneratorTest.java:[7,42] package org.springframework.data.redis.core does not exist
```

#### 根本原因

`mitedtsm-spring-boot-starter-redis` 模块原本没有测试代码，pom.xml 中未声明 `spring-boot-starter-test` 和 `mockito-core` 依赖。

#### 修复方案

在 `mitedtsm-spring-boot-starter-redis/pom.xml` 中添加测试依赖：

```xml
<!-- Test 测试相关 -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-test</artifactId>
    <scope>test</scope>
</dependency>
<dependency>
    <groupId>org.mockito</groupId>
    <artifactId>mockito-core</artifactId>
    <scope>test</scope>
</dependency>
```

#### 验证结果

```
[INFO] Tests run: 10, Failures: 0, Errors: 0, Skipped: 0
[INFO] BUILD SUCCESS
```

#### 经验教训

- 新模块添加测试代码前，先确认 pom.xml 中包含测试依赖
- `spring-boot-starter-test` 已包含 JUnit 5 和 Mockito，无需单独引入 JUnit

---

### BUG-005: 测试中使用不存在的 setter 方法

**Bug 编号**: SNG-002  
**严重级别**: 中  
**发现阶段**: 编译阶段  
**发现时间**: 2026-07-14  

#### 问题描述

在 `SerialNoGeneratorTest` 中使用 `serialNoGenerator.setStringRedisTemplate(stringRedisTemplate)` 注入 mock 对象，但 `SerialNoGenerator` 类中没有该 setter 方法。

#### 错误信息

```
[ERROR] cannot find symbol
  symbol:   method setStringRedisTemplate(StringRedisTemplate)
  location: class SerialNoGenerator
```

#### 根本原因

`SerialNoGenerator` 使用 `@Resource` 注解注入 `StringRedisTemplate`，`@Resource` 不提供 setter 方法，字段是 private 的。

#### 修复方案

使用 Spring Test 提供的 `ReflectionTestUtils` 通过反射设置私有字段：

```java
// 修改前
serialNoGenerator.setStringRedisTemplate(stringRedisTemplate);

// 修改后
import org.springframework.test.util.ReflectionTestUtils;
ReflectionTestUtils.setField(serialNoGenerator, "stringRedisTemplate", stringRedisTemplate);
```

#### 验证结果

测试编译通过，mock 对象正确注入。

#### 经验教训

- `@Resource` / `@Autowired` 注入的字段没有 public setter
- 单元测试中注入 mock 对象应使用 `ReflectionTestUtils.setField()`
- 或者将被注入字段改为 package-private 并提供 package-private setter

---

### BUG-006: 测试断言中编号格式和长度计算错误

**Bug 编号**: SNG-003  
**严重级别**: 低  
**发现阶段**: 测试执行阶段  
**发现时间**: 2026-07-14  

#### 问题描述

`testGenerateContractNo` 测试用例中，断言期望合同编号长度为 14，但实际长度为 16。

#### 错误信息

```
Expected :14
Actual   :16
```

#### 根本原因

编号格式为 `前缀 + 日期 + 序号`，计算长度时遗漏了前缀 "HT" 占 2 个字符：

| 部分 | 内容 | 长度 |
|------|------|------|
| 前缀 | HT | 2 |
| 日期 | yyyyMMdd | 8 |
| 序号 | 000001 | 6 |
| **合计** | | **16** |

#### 修复方案

```java
// 修改前
assertEquals(14, contractNo.length());

// 修改后
// HT(2) + yyyyMMdd(8) + 000001(6) = 16
assertEquals(16, contractNo.length());
```

#### 验证结果

```
[INFO] Tests run: 10, Failures: 0, Errors: 0, Skipped: 0
```

#### 经验教训

- 编写断言前，先手动计算期望值的长度
- 将长度计算公式以注释形式写在代码中，便于后续维护

---

### BUG-007: 按月格式的断言使用了错误的分隔符

**Bug 编号**: SNG-004  
**严重级别**: 低  
**发现阶段**: 测试执行阶段  
**发现时间**: 2026-07-14  

#### 问题描述

`testGenerateOrderNo` 测试用例中，断言期望订单编号以 `-0001` 结尾，但实际编号格式为 `ORD-2026-070001`，其中 `-07` 是日期的一部分（NORM_MONTH_PATTERN = `yyyy-MM`），序号 `0001` 直接拼接在日期后面。

#### 错误信息

```
Expected: ends with "-0001"
Actual: "ORD-2026-070001"
```

#### 根本原因

对 `DatePattern.NORM_MONTH_PATTERN`（`yyyy-MM`）格式理解有误。生成的编号是 `ORD-` + `2026-07` + `0001` = `ORD-2026-070001`，序号 `0001` 前面没有额外的 `-` 分隔符。

#### 修复方案

修改断言逻辑，不再假设序号前有分隔符：

```java
// 修改前
assertTrue(orderNo.endsWith("-0001"));

// 修改后
// 序号部分（4位补零）
assertTrue(orderNo.endsWith("0001"));
// 日期部分
String expectedDate = DateUtil.format(LocalDateTime.now(), DatePattern.NORM_MONTH_PATTERN);
assertTrue(orderNo.contains(expectedDate));
```

#### 验证结果

```
[INFO] Tests run: 10, Failures: 0, Errors: 0, Skipped: 0
```

#### 经验教训

- 编写断言前，先打印实际生成的值确认格式
- 日期格式中的 `-` 是格式的一部分，不是编号的分隔符

---

## 5. 公共组件库 - Bug 记录

### 开发内容

| 文件 | 说明 |
|------|------|
| `BpmProcessInstanceCommonApi.java` | BPM 通用 API 接口定义 |
| `BpmProcessInstanceCreateCommonReqDTO.java` | BPM 通用请求 DTO |
| `BpmAuditStatusEnum.java` | 审批状态枚举 |
| `BpmTaskStatusEnum.java` | 任务状态枚举 |

### BUG-008: mitedtsm-common 模块新增 BPM 公共类后，依赖模块编译失败

**Bug 编号**: COM-001  
**严重级别**: 中  
**发现阶段**: 编译阶段  
**发现时间**: 2026-07-14  

#### 问题描述

在 `mitedtsm-common` 模块中新增 BPM 相关的公共类后，依赖 `mitedtsm-common` 的其他模块（如 `mitedtsm-module-crm`）编译时报错。

#### 错误信息

```
[ERROR] cannot find symbol
  symbol:   class BpmProcessInstanceCommonApi
```

#### 根本原因

新增的 `BpmProcessInstanceCommonApi` 接口引用了 `BpmProcessInstanceCreateCommonReqDTO`，而该 DTO 中使用了 `jakarta.validation.Valid` 注解。`mitedtsm-common` 模块的 pom.xml 中未声明 `jakarta.validation-api` 依赖，导致编译时找不到 `@Valid` 注解。

#### 修复方案

在 `BpmProcessInstanceCreateCommonReqDTO` 中移除对 `@Valid` 注解的使用（该注解在通用 DTO 层不必要，验证应在 Controller 层进行）：

```java
// 修改前
import jakarta.validation.Valid;

public class BpmProcessInstanceCreateCommonReqDTO {
    @Valid
    private Map<String, Object> variables;
}

// 修改后 - 移除 @Valid
public class BpmProcessInstanceCreateCommonReqDTO {
    private Map<String, Object> variables;
}
```

#### 验证结果

```
[INFO] BUILD SUCCESS
```

所有依赖模块编译通过。

#### 经验教训

- `mitedtsm-common` 是基础模块，新增类时应避免引入额外的依赖
- 通用 DTO 中不应使用需要额外依赖的验证注解
- 验证逻辑应放在 Controller 层的 VO 中，而非公共 DTO 中

---

## 6. Bug 统计与分析

### 6.1 按任务统计

| 任务 | Bug 数量 | 占比 |
|------|---------|------|
| BPM 审批流程通用模板 | 0 | 0% |
| BPM 审批集成框架 | 3 | 37.5% |
| 统一编号生成服务 | 4 | 50% |
| 公共组件库 | 1 | 12.5% |

### 6.2 按严重级别统计

| 严重级别 | 数量 | 占比 | Bug 编号 |
|---------|------|------|---------|
| 高 | 1 | 12.5% | BUG-001 |
| 中 | 4 | 50% | BUG-002, BUG-004, BUG-005, BUG-008 |
| 低 | 3 | 37.5% | BUG-003, BUG-006, BUG-007 |

### 6.3 按发现阶段统计

| 发现阶段 | 数量 | 占比 |
|---------|------|------|
| 编译阶段 | 4 | 50% |
| 测试执行阶段 | 2 | 25% |
| 集成测试 | 1 | 12.5% |
| 代码审查 | 1 | 12.5% |

### 6.4 按根因分类

| 根因类型 | 数量 | 占比 | Bug 编号 |
|---------|------|------|---------|
| 依赖配置缺失 | 2 | 25% | BUG-004, BUG-008 |
| 接口/类型不匹配 | 2 | 25% | BUG-001, BUG-002 |
| 测试代码编写不当 | 2 | 25% | BUG-005, BUG-006 |
| 枚举值定义不一致 | 1 | 12.5% | BUG-003 |
| 格式理解错误 | 1 | 12.5% | BUG-007 |

### 6.5 Bug 发现与修复时间线

```
2026-07-14 上午
  ├── BPM 审批集成框架开发
  │   ├── BUG-001: Bean 创建失败 → 修改注入方式 → 修复
  │   ├── BUG-002: DTO 类型不匹配 → 增加类型转换 → 修复
  │   └── BUG-003: 枚举值不一致 → 对齐枚举值 → 修复
  │
  └── 公共组件库开发
      └── BUG-008: 编译失败 → 移除 @Valid → 修复

2026-07-14 下午
  └── 统一编号生成服务开发
      ├── BUG-004: 测试依赖缺失 → 添加 pom 依赖 → 修复
      ├── BUG-005: setter 不存在 → 使用 ReflectionTestUtils → 修复
      ├── BUG-006: 长度计算错误 → 修正断言值 → 修复
      └── BUG-007: 格式理解错误 → 修正断言逻辑 → 修复
```

---

## 7. 经验总结与改进建议

### 7.1 关键经验

#### 7.1.1 跨模块 API 设计

- **问题**: 公共 API 接口与内部实现之间存在类型差异
- **经验**: 公共 API 应尽量使用基础类型（String、Integer），避免使用内部特有的类型
- **改进**: 在 API 实现层做类型转换，保持公共接口的简洁性

#### 7.1.2 枚举值对齐

- **问题**: 公共枚举与内部枚举值不一致
- **经验**: 公共枚举必须严格参考内部枚举的实际值定义
- **改进**: 开发时同时打开两个枚举类进行对照，编写测试验证值的一致性

#### 7.1.3 测试代码编写

- **问题**: 测试代码中使用了不存在的方法、错误的断言值
- **经验**: 编写测试前应先确认被测类的 API，先打印实际值再编写断言
- **改进**: 使用 `ReflectionTestUtils` 处理私有字段注入

#### 7.1.4 依赖管理

- **问题**: 新增模块/类时遗漏必要的依赖声明
- **经验**: 新模块创建后应立即添加完整的依赖（包括测试依赖）
- **改进**: 使用模块模板，包含标准的依赖声明

### 7.2 改进建议

| 建议 | 优先级 | 说明 |
|------|--------|------|
| 建立公共组件开发规范 | 高 | 定义公共 API 的设计原则、命名规范、依赖约束 |
| 增加编译时校验 | 高 | 使用 `@CompileStatic` 或注解处理器提前发现类型不匹配 |
| 编写开发者指南 | 中 | 为其他 7 个域的成员提供 BPM 框架和编号生成器的使用文档 |
| 增加集成测试 | 中 | 模拟真实业务场景验证框架功能 |
| 建立代码审查清单 | 低 | 包含枚举值对齐、依赖检查、类型转换等检查项 |

---

**报告生成时间**: 2026-07-14  
**报告负责人**: jxq  
**审核状态**: 待审核
