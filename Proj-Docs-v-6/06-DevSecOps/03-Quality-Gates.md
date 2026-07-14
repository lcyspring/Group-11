# 代码质量门禁体系 - 密讯ETM系统 (mitedtsm)

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

## 1. 质量门禁体系总览

```
┌──────────────────────────────────────────────────────────────────────┐
│                        代码质量门禁体系全景图                           │
└──────────────────────────────────────────────────────────────────────┘

 阶段一              阶段二              阶段三              阶段四
 编码规范            静态分析            测试验证            构建部署
 ────────            ────────            ────────            ────────

┌──────────┐    ┌──────────────┐    ┌──────────────┐    ┌──────────────┐
│ Checkstyle│    │ SonarQube    │    │ 单元测试      │    │ Docker 构建  │
│ ESLint   │───▶│ 质量门禁      │───▶│ 覆盖率 ≥ 70% │───▶│ 镜像扫描      │
│ Prettier │    │ 安全规则      │    │ 集成测试      │    │ 部署就绪      │
└──────────┘    └──────────────┘    └──────────────┘    └──────────────┘
     │                │                   │                   │
     ▼                ▼                   ▼                   ▼
 不通过驳回        不通过驳回          不通过驳回           不通过驳回
 本地修复          本地修复            补充测试             修复问题
```

---

## 2. Checkstyle 配置（Java）

### 2.1 关键规则

```xml
<?xml version="1.0"?>
<!DOCTYPE module PUBLIC
    "-//Checkstyle//DTD Checkstyle Configuration 1.3//EN"
    "https://checkstyle.org/dtds/configuration_1_3.dtd">

<module name="Checker">
    <property name="charset" value="UTF-8"/>
    <property name="severity" value="error"/>
    <property name="fileExtensions" value="java, properties, xml"/>

    <!-- 文件级别检查 -->
    <module name="FileLength">
        <property name="max" value="2000"/>
    </module>
    <module name="FileTabCharacter">
        <property name="eachLine" value="true"/>
    </module>
    <module name="LineLength">
        <property name="max" value="120"/>
        <property name="ignorePattern" value="^package.*|^import.*"/>
    </module>

    <module name="TreeWalker">
        <!-- 导入规范 -->
        <module name="AvoidStarImport"/>
        <module name="RedundantImport"/>
        <module name="UnusedImports"/>
        <module name="IllegalImport">
            <property name="illegalPkgs" value="sun.*, com.sun.*"/>
        </module>

        <!-- 命名规范 -->
        <module name="ConstantName">
            <property name="format" value="^[A-Z][A-Z0-9]*(_[A-Z0-9]+)*$"/>
        </module>
        <module name="LocalVariableName">
            <property name="format" value="^[a-z][a-zA-Z0-9]*$"/>
        </module>
        <module name="MemberName">
            <property name="format" value="^[a-z][a-zA-Z0-9]*$"/>
        </module>
        <module name="MethodName">
            <property name="format" value="^[a-z][a-zA-Z0-9]*$"/>
        </module>
        <module name="ParameterName">
            <property name="format" value="^[a-z][a-zA-Z0-9]*$"/>
        </module>
        <module name="TypeName">
            <property name="format" value="^[A-Z][a-zA-Z0-9]*$"/>
        </module>
        <module name="PackageName">
            <property name="format" value="^[a-z]+(\.[a-z][a-z0-9]*)*$"/>
        </module>

        <!-- 代码结构 -->
        <module name="EmptyBlock"/>
        <module name="NeedBraces"/>
        <module name="LeftCurly"/>
        <module name="RightCurly"/>
        <module name="EqualsHashCode"/>
        <module name="HiddenField">
            <property name="ignoreSetter" value="true"/>
            <property name="ignoreConstructorParameter" value="true"/>
        </module>
        <module name="MissingSwitchDefault"/>
        <module name="MultipleVariableDeclarations"/>
        <module name="SimplifyBooleanExpression"/>
        <module name="SimplifyBooleanReturn"/>

        <!-- 方法设计 -->
        <module name="MethodLength">
            <property name="max" value="80"/>
            <property name="countEmpty" value="false"/>
        </module>
        <module name="ParameterNumber">
            <property name="max" value="7"/>
        </module>

        <!-- 空白规范 -->
        <module name="GenericWhitespace"/>
        <module name="MethodParamPad"/>
        <module name="NoWhitespaceAfter"/>
        <module name="NoWhitespaceBefore"/>
        <module name="WhitespaceAfter"/>
        <module name="WhitespaceAround"/>

        <!-- 注解 -->
        <module name="MissingOverride"/>
        <module name="MissingDeprecated"/>
    </module>
</module>
```

### 2.2 Maven 集成

```xml
<plugin>
    <groupId>org.apache.maven.plugins</groupId>
    <artifactId>maven-checkstyle-plugin</artifactId>
    <version>3.3.1</version>
    <configuration>
        <configLocation>checkstyle.xml</configLocation>
        <encoding>UTF-8</encoding>
        <consoleOutput>true</consoleOutput>
        <failsOnError>true</failsOnError>
    </configuration>
    <executions>
        <execution>
            <id>validate</id>
            <phase>validate</phase>
            <goals><goal>check</goal></goals>
        </execution>
    </executions>
</plugin>
```

---

## 3. ESLint 配置（TypeScript / Vue）

### 3.1 核心规则

```javascript
module.exports = {
  root: true,
  env: {
    browser: true,
    es2022: true,
    node: true,
  },
  extends: [
    'eslint:recommended',
    'plugin:@typescript-eslint/recommended',
    'plugin:vue/vue3-recommended',
    'prettier',
  ],
  parser: 'vue-eslint-parser',
  parserOptions: {
    parser: '@typescript-eslint/parser',
    ecmaVersion: 'latest',
    sourceType: 'module',
    extraFileExtensions: ['.vue'],
  },
  rules: {
    // TypeScript 规则
    '@typescript-eslint/no-explicit-any': 'error',           // 禁止 any
    '@typescript-eslint/no-unused-vars': ['error', {
      argsIgnorePattern: '^_',
      varsIgnorePattern: '^_',
    }],
    '@typescript-eslint/consistent-type-imports': 'error',
    '@typescript-eslint/no-floating-promises': 'error',
    '@typescript-eslint/ban-ts-comment': 'error',

    // Vue 规则
    'vue/multi-word-component-names': 'error',
    'vue/no-unused-vars': 'error',
    'vue/no-mutating-props': 'error',
    'vue/no-v-html': 'warn',
    'vue/require-default-prop': 'error',
    'vue/require-prop-types': 'error',
    'vue/no-side-effects-in-computed-properties': 'error',
    'vue/require-explicit-emits': 'error',

    // 通用规则
    'no-console': ['warn', { allow: ['warn', 'error'] }],
    'no-debugger': 'error',
    'no-var': 'error',
    'prefer-const': 'error',
    'eqeqeq': ['error', 'always'],
    'curly': ['error', 'all'],
    'no-multiple-empty-lines': ['error', { max: 1 }],
    'no-trailing-spaces': 'error',
    'comma-dangle': ['error', 'always-multiline'],
    'semi': ['error', 'always'],
    'quotes': ['error', 'single', { avoidEscape: true }],
    'object-shorthand': 'error',
  },
};
```

### 3.2 Prettier 配置

```json
{
  "semi": true,
  "singleQuote": true,
  "trailingComma": "all",
  "printWidth": 100,
  "tabWidth": 2,
  "useTabs": false,
  "bracketSpacing": true,
  "arrowParens": "always",
  "endOfLine": "lf",
  "vueIndentScriptAndStyle": false
}
```

---

## 4. 单元测试覆盖率 ≥ 70%

### 4.1 覆盖率计算规则

```
覆盖率 = (已覆盖行数 / 可覆盖行数) × 100%

覆盖标准：
- 行覆盖率（Line Coverage）：≥ 70%
- 分支覆盖率（Branch Coverage）：≥ 60%
- 方法覆盖率（Method Coverage）：≥ 70%
```

### 4.2 后端测试示例（JUnit 5 + Mockito）

```java
@ExtendWith(MockitoExtension.class)
class CustomerServiceTest {

    @Mock
    private CustomerMapper customerMapper;

    @InjectMocks
    private CustomerServiceImpl customerService;

    @Test
    @DisplayName("正常创建客户 - 成功")
    void shouldCreateCustomerSuccessfully() {
        CustomerDTO dto = new CustomerDTO();
        dto.setName("测试客户");
        dto.setTenantId(1L);

        when(customerMapper.insert(any())).thenReturn(1);

        Result<CustomerVO> result = customerService.createCustomer(dto);

        assertThat(result.isSuccess()).isTrue();
        verify(customerMapper).insert(any());
    }

    @Test
    @DisplayName("创建客户 - 名称为空应失败")
    void shouldFailWhenNameIsEmpty() {
        CustomerDTO dto = new CustomerDTO();
        dto.setName("");

        assertThrows(BusinessException.class, () ->
            customerService.createCustomer(dto)
        );
    }
}
```

### 4.3 前端测试示例（Vitest）

```typescript
import { describe, it, expect, vi } from 'vitest';
import { mount } from '@vue/test-utils';
import CustomerList from '@/views/crm/CustomerList.vue';

describe('CustomerList', () => {
  it('should render customer table', () => {
    const wrapper = mount(CustomerList, {
      props: {
        customers: [
          { id: 1, name: '客户A', industry: 'IT' },
          { id: 2, name: '客户B', industry: '金融' },
        ],
      },
    });

    expect(wrapper.findAll('tr')).toHaveLength(3);
    expect(wrapper.text()).toContain('客户A');
    expect(wrapper.text()).toContain('客户B');
  });

  it('should show empty state when no customers', () => {
    const wrapper = mount(CustomerList, {
      props: { customers: [] },
    });
    expect(wrapper.text()).toContain('暂无数据');
  });
});
```

---

## 5. 代码审查检查清单

| 序号 | 检查项 | 分类 | 说明 |
|------|-------|------|------|
| 1 | 功能正确性 | 功能 | 代码实现了预期的功能需求，逻辑正确 |
| 2 | 边界条件处理 | 功能 | null 值、空集合、零值、负数等边界情况已处理 |
| 3 | 异常处理 | 功能 | 异常被正确捕获和处理 |
| 4 | 并发安全 | 功能 | 共享资源访问有适当的同步机制 |
| 5 | SQL 注入防护 | 安全 | 所有数据库操作使用 MyBatis-Plus 参数化查询 |
| 6 | XSS 防护 | 安全 | 用户输入输出经过正确编码/转义 |
| 7 | 权限控制 | 安全 | 敏感接口有 @PreAuthorize 权限注解 |
| 8 | 多租户隔离 | 安全 | 租户数据隔离已验证，DO 类正确继承基类 |
| 9 | 敏感信息泄露 | 安全 | 日志中无密码、密钥等敏感信息 |
| 10 | 数据库查询效率 | 性能 | 避免 N+1 查询、全表扫描 |
| 11 | 内存使用 | 性能 | 无明显内存泄漏 |
| 12 | 代码可读性 | 质量 | 变量命名清晰、方法职责单一 |
| 13 | 代码重复 | 质量 | 无明显重复代码，公共逻辑已提取 |
| 14 | 单元测试覆盖 | 质量 | 新增代码单元测试覆盖率 ≥ 70% |
| 15 | i18n 国际化 | 规范 | 新增文本已添加多语言资源文件 |

---

## 6. 性能测试门禁

| 指标 | 目标值 | 告警阈值 | 阻断阈值 |
|------|--------|---------|---------|
| API 平均响应时间 | ≤ 200ms | > 500ms | > 1000ms |
| API P95 响应时间 | ≤ 500ms | > 1000ms | > 2000ms |
| TPS | ≥ 500 | < 300 | < 100 |
| 错误率 | ≤ 0.1% | > 0.5% | > 1% |
| 并发用户数 | ≥ 200 | < 100 | < 50 |
| CPU 使用率 | ≤ 70% | > 85% | > 95% |
| 内存使用率 | ≤ 80% | > 90% | > 95% |
| 数据库连接池 | ≤ 50% | > 80% | > 95% |

---

## 7. 质量门禁决策矩阵

| 门禁项 | 通过条件 | 不通过后果 | 是否可跳过 |
|--------|---------|-----------|-----------|
| Checkstyle / ESLint | 0 error | 构建失败 | 不可跳过 |
| SonarQube 质量门禁 | 全部条件通过 | 合并阻止 | 不可跳过 |
| 单元测试 | 全部通过 | 合并阻止 | 不可跳过 |
| 覆盖率 ≥ 70% | 达标 | 合并阻止 | Tech Lead 特批 |
| OWASP 依赖扫描 | 无 ≥ 7.0 CVSS | 合并阻止 | 安全负责人特批 |
| 镜像扫描 | 无 HIGH/CRITICAL | 部署阻止 | 安全负责人特批 |
| Code Review | ≥ 1 人 Approve | 合并阻止 | 不可跳过 |
| 性能测试 | 指标达标 | 发布阻止 | PM 审批 |

---

## 8. 质量度量指标

| 指标 | 计算方式 | 目标值 |
|------|---------|--------|
| 质量门禁通过率 | 通过次数 / 总执行次数 | ≥ 95% |
| 代码覆盖率趋势 | 周度覆盖率平均值 | 持续上升 |
| 技术债务比率 | 修复成本 / 开发成本 | ≤ 5% |
| 缺陷密度 | 缺陷数 / 千行代码 | ≤ 2 |
| 代码审查覆盖率 | 已审查 PR / 总 PR 数 | 100% |
| 平均修复时间 | 缺陷创建到关闭时间 | ≤ 24h |

---

> **文档维护**: 本文档由 DevSecOps 团队维护，质量门禁变更需经过 Tech Lead 审批。
