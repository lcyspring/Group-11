# SonarQube 质量门禁配置 - 密讯ETM系统 (mitedtsm)

---

## 文档信息

| 字段 | 内容 |
|------|------|
| 项目名称 | 密讯ETM企业管理系统 (mitedtsm) |
| 文档版本 | v1.0 |
| 创建日期 | 2026-06-25 |
| 负责人 | DevSecOps 团队 |
| 状态 | 已发布 |

---

## 1. SonarQube 质量门禁定义

```yaml
quality_gate:
  name: "mitedtsm Quality Gate"
  description: "密讯ETM系统代码质量门禁"

  metrics:
    # ===== 阻断级 =====
    - metric: new_blocker_violations
      operator: GT
      value: 0
      on_leak_period: true

    - metric: new_critical_violations
      operator: GT
      value: 0
      on_leak_period: true

    - metric: new_security_hotspots
      operator: GT
      value: 0
      on_leak_period: true

    - metric: new_vulnerabilities
      operator: GT
      value: 0
      on_leak_period: true

    - metric: new_bugs
      operator: GT
      value: 0
      on_leak_period: true

    # ===== 覆盖率 =====
    - metric: new_coverage
      operator: LT
      value: 70
      on_leak_period: true

    - metric: new_line_coverage
      operator: LT
      value: 70
      on_leak_period: true

    - metric: new_branch_coverage
      operator: LT
      value: 60
      on_leak_period: true

    # ===== 代码异味 =====
    - metric: new_code_smells
      operator: GT
      value: 10
      on_leak_period: true

    # ===== 重复代码 =====
    - metric: new_duplicated_lines_density
      operator: GT
      value: 3
      on_leak_period: true

    # ===== 技术债务 =====
    - metric: new_technical_debt_ratio
      operator: GT
      value: 5
      on_leak_period: true

    # ===== 可靠性 =====
    - metric: new_reliability_rating
      operator: GT
      value: 3              # A=1, B=2, C=3, D=4, E=5
      on_leak_period: true

    # ===== 安全性 =====
    - metric: new_security_rating
      operator: GT
      value: 3
      on_leak_period: true

    # ===== 可维护性 =====
    - metric: new_maintainability_rating
      operator: GT
      value: 3
      on_leak_period: true
```

---

## 2. Java 代码规则

| 编号 | 规则 Key | 规则名称 | 严重级别 | 说明 |
|------|---------|---------|---------|------|
| J01 | `java:S1192` | 字符串字面量重复 | Critical | 同一字符串出现 ≥ 3 次，应提取为常量 |
| J02 | `java:S119` | 类型参数命名规范 | Major | 泛型类型参数使用单字母大写命名 |
| J03 | `java:S1144` | 未使用的私有方法 | Major | 删除未被调用的私有方法 |
| J04 | `java:S106` | 禁止标准输出 | Major | 生产代码禁止 System.out/System.err |
| J05 | `java:S112` | 异常处理规范 | Major | 禁止抛出通用异常（Exception/RuntimeException） |
| J06 | `java:S1186` | 空方法体 | Critical | 方法体为空必须添加注释说明原因 |
| J07 | `java:S115` | 常量命名规范 | Critical | static final 常量使用大写+下划线 |
| J08 | `java:S116` | 字段命名规范 | Major | 非 final 字段遵循驼峰命名 |
| J09 | `java:S1319` | 集合类型声明 | Minor | 声明使用接口类型（List）而非实现类（ArrayList） |
| J10 | `java:S2142` | 线程中断异常 | Major | InterruptedException 不应被吞没 |
| J11 | `java:S2068` | 硬编码密码 | Blocker | 禁止代码中硬编码密码字面量 |
| J12 | `java:S2077` | SQL 注入风险 | Blocker | 禁止动态拼接 SQL 查询 |
| J13 | `java:S1172` | 未使用的方法参数 | Major | 删除未使用的方法参数 |
| J14 | `java:S1135` | TODO 标记 | Info | 完成所有 TODO 或创建跟踪工单 |
| J15 | `java:S1125` | 冗余布尔字面量 | Minor | 移除不必要的布尔字面量 |

### 自定义规则补充

| 编号 | 规则 | 严重级别 | 说明 |
|------|------|---------|------|
| J16 | 方法圈复杂度 ≤ 15 | Major | 超过需重构拆分 |
| J17 | 方法行数 ≤ 80 行 | Major | 超过需提取方法 |
| J18 | 类行数 ≤ 500 行 | Major | 超过需拆分类 |
| J19 | DO 类继承规范 | Critical | 租户表 DO 必须继承 TenantBaseDO |
| J20 | MapStruct Convert 规范 | Major | Convert 接口必须标注 @Mapper(componentModel = "spring") |

---

## 3. TypeScript / Vue 代码规则

| 编号 | 规则 Key | 规则名称 | 严重级别 | 说明 |
|------|---------|---------|---------|------|
| T01 | `typescript:S1128` | 未使用的导入 | Major | 删除未使用的 import |
| T02 | `typescript:S1854` | 未使用的变量 | Major | 删除声明但未使用的变量 |
| T03 | `typescript:S4325` | 冗余类型断言 | Minor | 移除不必要的类型断言 |
| T04 | `typescript:S4124` | 禁止 any 类型 | Major | 使用明确类型替代 any |
| T05 | `typescript:S109` | 魔法数字 | Major | 使用命名常量替代魔法数字 |
| T06 | `typescript:S3776` | 圈复杂度 ≤ 15 | Critical | 函数圈复杂度控制在 15 以内 |
| T07 | `typescript:S138` | 函数行数 ≤ 80 | Major | 函数体不超过 80 行 |
| T08 | `typescript:S1541` | 参数数量 ≤ 7 | Major | 函数参数不超过 7 个 |
| T09 | `typescript:S1874` | 废弃 API 使用 | Major | 禁止使用 @deprecated API |
| T10 | `typescript:S131` | Switch 缺 default | Critical | switch 必须有 default 分支 |
| T11 | `typescript:S1067` | 复杂条件表达式 | Critical | 条件嵌套不超过 3 层 |
| T12 | `typescript:S1134` | 测试断言 | Major | 测试方法至少包含一个断言 |
| T13 | `typescript:S881` | 自增/自减 | Minor | 避免 ++/--，使用 += 1 |
| T14 | `typescript:S2681` | 嵌套块深度 | Major | 代码块嵌套不超过 4 层 |

### Vue 特定规则

| 规则 | 级别 | 说明 |
|------|------|------|
| `vue/multi-word-component-names` | error | 组件名多单词 |
| `vue/no-unused-vars` | error | 未使用的变量 |
| `vue/no-mutating-props` | error | 禁止修改 props |
| `vue/no-v-html` | warn | 谨慎使用 v-html（XSS 风险） |
| `vue/require-default-prop` | error | props 必须有默认值 |
| `vue/require-prop-types` | error | props 必须声明类型 |
| `vue/no-side-effects-in-computed` | error | computed 无副作用 |
| `vue/require-explicit-emits` | error | emit 必须显式声明 |
| `vue/component-tags-order` | warn | template → script → style 顺序 |

---

## 4. 复杂度阈值

| 指标 | 阈值 | 说明 |
|------|------|------|
| 方法圈复杂度 | ≤ 15 | 每个 if/for/while/case/catch/&&/|| 加 1 |
| 类圈复杂度（加权） | ≤ 200 | 超过需拆分类 |
| 方法行数 | ≤ 80 行 | 超过需提取方法 |
| 类行数 | ≤ 500 行 | 超过需拆分类 |
| 方法参数 | ≤ 7 个 | 超过需封装为对象 |
| 嵌套深度 | ≤ 4 层 | 超过需提取方法 |
| 认知复杂度 | ≤ 15 | 保持代码可读性 |

### 复杂度计算示例

```java
// 圈复杂度 = 基础值 1 + 每个分支点 +1

// 可接受 - 圈复杂度 = 3
public String getStatus(CustomerDO customer) {
    if (customer == null) {           // +1
        return "未知";
    }
    if (customer.isActive()
        && customer.isVerified()) {   // +2 (if + &&)
        return "活跃";
    }
    return "非活跃";
}

// 超标 - 圈复杂度 = 18，需重构拆分
public String processOrder(OrderDO order) {
    if (order == null) { ... }        // +1
    if (order.isPaid()) { ... }       // +1
    else if (order.isPending()) { ... } // +1
    for (Item item : order.getItems()) { // +1
        if (item.isAvailable()) { ... }  // +1
        switch (item.getType()) {        // +1
            case A: ... break;           // +1
            case B: ... break;           // +1
            case C: ... break;           // +1
        }
    }
    // ... 更多分支
}
```

---

## 5. 覆盖率阈值

| 指标 | 整体目标 | 新增代码 | 核心模块 |
|------|---------|---------|---------|
| 行覆盖率（Line） | ≥ 70% | ≥ 80% | ≥ 85% |
| 分支覆盖率（Branch） | ≥ 60% | ≥ 70% | ≥ 80% |
| 方法覆盖率（Method） | ≥ 70% | ≥ 80% | ≥ 85% |

### 核心模块定义

| 模块 | 类型 | 原因 |
|------|------|------|
| `mitedtsm-module-system` | 核心 | 认证授权、租户管理，安全关键 |
| `mitedtsm-module-pay` | 核心 | 支付处理，资金安全关键 |
| `mitedtsm-module-bpm` | 核心 | 审批流引擎，业务流程关键 |
| `mitedtsm-module-erp` | 核心 | 财务计算，数据准确关键 |
| `mitedtsm-module-crm` | 标准 | 客户关系管理，业务关键 |
| `mitedtsm-module-mall` | 标准 | 商城业务 |
| `mitedtsm-module-wms` | 标准 | 仓库管理 |
| `mitedtsm-module-mes` | 标准 | 制造执行 |
| `mitedtsm-module-infra` | 标准 | 基础设施 |
| `mitedtsm-module-report` | 标准 | 报表中心 |

### JaCoCo 排除规则

```xml
<configuration>
    <excludes>
        <!-- 数据传输对象 -->
        <exclude>**/dto/**</exclude>
        <exclude>**/vo/**</exclude>
        <!-- DO 实体类（Lombok @Data 自动生成 getter/setter） -->
        <exclude>**/dataobject/**</exclude>
        <!-- 配置类 -->
        <exclude>**/config/**</exclude>
        <!-- 常量/枚举 -->
        <exclude>**/enums/**</exclude>
        <!-- 启动类 -->
        <exclude>**/*Application.java</exclude>
        <exclude>**/*ServerApplication.java</exclude>
        <!-- 转换器接口（MapStruct 自动生成） -->
        <exclude>**/convert/**</exclude>
    </excludes>
</configuration>
```

---

## 6. sonar-project.properties 配置

### 6.1 后端配置

```properties
# 密讯ETM系统 - 后端 SonarQube 配置
sonar.projectKey=mitedtsm-server
sonar.projectName=mitedtsm Server
sonar.projectVersion=2026.01
sonar.projectDescription=密讯ETM系统 - 后端服务

# 源代码目录
sonar.sources=src/main/java
sonar.tests=src/test/java

# 编译产物
sonar.java.binaries=target/classes
sonar.java.source=17
sonar.sourceEncoding=UTF-8

# 覆盖率报告
sonar.coverage.jacoco.xmlReportPaths=target/site/jacoco/jacoco.xml

# 排除
sonar.coverage.exclusions=**/dto/**,**/dataobject/**,**/vo/**,**/config/**,**/enums/**,**/convert/**,**/*Application.java
sonar.exclusions=**/node_modules/**,**/target/**,**/*.xml

# 测试文件
sonar.test.inclusions=**/*Test.java,**/*Tests.java

# 重复代码排除
sonar.cpd.exclusions=**/dto/**,**/dataobject/**,**/vo/**

# 质量门禁
sonar.qualitygate.wait=true
sonar.qualitygate.timeout=300
```

### 6.2 Admin Web 前端配置

```properties
# 密讯ETM系统 - Admin Web 前端 SonarQube 配置
sonar.projectKey=mitedtsm-web
sonar.projectName=mitedtsm Admin Web
sonar.projectVersion=2026.01
sonar.projectDescription=密讯ETM系统 - 管理后台前端

# 源代码目录
sonar.sources=src
sonar.tests=src

# 编码
sonar.sourceEncoding=UTF-8

# 测试文件
sonar.test.inclusions=**/*.spec.ts,**/*.test.ts

# 排除
sonar.exclusions=**/node_modules/**,**/dist/**,**/coverage/**,**/*.d.ts,**/vite.config.ts

# 覆盖率
sonar.javascript.lcov.reportPaths=coverage/lcov.info

# 重复代码排除
sonar.cpd.exclusions=**/*.spec.ts,**/*.test.ts

# 质量门禁
sonar.qualitygate.wait=true
sonar.qualitygate.timeout=300
```

---

## 7. 质量门禁执行流程

```
┌───────────────────────────────────────────────────────────────┐
│                    SonarQube 质量门禁流程                       │
└───────────────────────────────────────────────────────────────┘

 代码提交
     │
     ▼
 ┌─────────┐     ┌──────────────────┐     ┌──────────────────┐
 │  CI 触发 │────▶│ SonarQube 扫描    │────▶│ 质量门禁评估      │
 │ 扫描任务 │     │ 全量/增量分析     │     │ 对比阈值          │
 └─────────┘     └──────────────────┘     └──────────────────┘
                                                │
                               ┌────────────────┼────────────────┐
                               ▼                ▼                ▼
                         ┌──────────┐    ┌──────────┐    ┌──────────┐
                         │ 通过      │    │ 警告      │    │ 失败      │
                         │ 允许合并  │    │ 建议修复  │    │ 阻断流水线 │
                         └──────────┘    └──────────┘    └──────────┘
```

---

## 8. 常见问题与解决方案

| 问题 | 原因 | 解决方案 |
|------|------|---------|
| 覆盖率不达标 | 缺少测试 | 补充单元测试，优先覆盖核心业务逻辑 |
| 重复代码超标 | 多处相似逻辑 | 提取公共方法或工具类 |
| 圈复杂度超标 | 方法过长或分支过多 | 拆分为多个小方法，使用策略模式 |
| 安全漏洞 | 不安全的 API 使用 | 按安全编码规范修复 |
| 技术债务过高 | 长期积累 | 分阶段重构，每次迭代降低 5% |
| 误报 | 规则不适用 | 使用 @SuppressWarnings 或 //NOSONAR 忽略 |
| DO 类继承警告 | 未正确继承基类 | 租户表 DO 继承 TenantBaseDO，共享表继承 BaseDO |

---

> **文档维护**: 本文档由 DevSecOps 团队维护，规则变更需经过 Tech Lead 审批。
