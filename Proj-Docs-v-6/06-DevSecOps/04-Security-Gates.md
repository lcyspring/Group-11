# 安全门禁策略 - 密讯ETM系统 (mitedtsm)

---

## 文档信息

| 字段 | 内容 |
|------|------|
| 项目名称 | 密讯ETM企业管理系统 (mitedtsm) |
| 文档版本 | v1.0 |
| 创建日期 | 2026-06-25 |
| 负责人 | DevSecOps 团队 / 安全负责人 |
| 状态 | 已发布 |

---

## 1. 安全门禁体系总览

```
┌────────────────────────────────────────────────────────────────────────────┐
│                         安全门禁流水线（Security Gates）                      │
└────────────────────────────────────────────────────────────────────────────┘

 代码提交        SAST 扫描        依赖扫描        镜像扫描        密钥扫描
 ────────       ─────────        ────────        ────────        ────────

┌────────┐   ┌──────────────┐  ┌────────────┐  ┌───────────┐  ┌──────────┐
│ 开发者  │   │ SonarQube    │  │ OWASP      │  │ Trivy      │  │ GitLeaks │
│ Push   │──▶│ 静态代码分析  │─▶│ 依赖检查    │─▶│ 镜像扫描    │─▶│ 密钥泄露  │
│        │   │              │  │            │  │            │  │ 扫描     │
└────────┘   └──────────────┘  └────────────┘  └───────────┘  └──────────┘
                    │                 │               │              │
                    ▼                 ▼               ▼              ▼
              ┌──────────┐    ┌──────────┐    ┌──────────┐    ┌──────────┐
              │ 阻断      │    │ 阻断      │    │ 阻断      │    │ 阻断      │
              │ Bug/漏洞  │    │ HIGH/     │    │ HIGH/     │    │ 发现密钥  │
              │ 需修复    │    │ CRITICAL  │    │ CRITICAL  │    │ 立即轮换  │
              └──────────┘    └──────────┘    └──────────┘    └──────────┘

所有门禁通过 → 允许合并 / 允许部署
任一门禁阻断 → 流水线失败，禁止继续
```

---

## 2. SAST 配置（SonarQube 静态应用安全测试）

### 2.1 扫描触发条件

| 触发条件 | 扫描范围 | 阻断规则 |
|---------|---------|---------|
| Feature 分支推送 | 增量代码 | Bug ≥ 1 个 |
| Merge Request | 增量代码 | Bug ≥ 1 个 |
| develop 分支 | 全量代码 | Bug > 0 个 |
| release 分支 | 全量代码 | Bug > 0 个 |
| 定时扫描（每日） | 全量代码 | 告警通知 |

### 2.2 安全规则分类

| 规则分类 | 严重级别 | 示例 |
|---------|---------|------|
| SQL 注入 | Blocker | 动态拼接 SQL（禁止 `.apply()` 直接拼接用户输入） |
| XSS 跨站脚本 | Blocker | 未转义用户输入、v-html 直接渲染 |
| 硬编码密码 | Blocker | 代码/配置文件中明文密码 |
| 命令注入 | Blocker | Runtime.exec 未过滤 |
| XXE 外部实体 | Critical | XML 解析未禁用外部实体 |
| 路径遍历 | Critical | 文件路径未校验 |
| 不安全的反序列化 | Critical | ObjectInputStream 无过滤 |
| 弱加密算法 | Major | DES / MD5 / SHA-1（应使用 BCrypt / AES-256） |
| 日志注入 | Major | 日志中直接输出用户输入 |
| 敏感信息泄露 | Major | 异常堆栈返回前端 |
| 租户数据越权 | Critical | 未使用 TenantBaseDO 或缺少 @TenantIgnore 误用 |

---

## 3. 依赖扫描（OWASP Dependency-Check）

### 3.1 Maven 插件配置

```xml
<plugin>
    <groupId>org.owasp</groupId>
    <artifactId>dependency-check-maven</artifactId>
    <version>9.2.0</version>
    <configuration>
        <failBuildOnCVSS>7</failBuildOnCVSS>
        <formats>
            <format>HTML</format>
            <format>JSON</format>
        </formats>
        <suppressionFiles>
            <suppressionFile>owasp-suppressions.xml</suppressionFile>
        </suppressionFiles>
    </configuration>
    <executions>
        <execution>
            <goals><goal>check</goal></goals>
        </execution>
    </executions>
</plugin>
```

### 3.2 前端依赖扫描

```bash
# pnpm audit 检查前端依赖漏洞
pnpm audit --audit-level=high

# Snyk 深度扫描
npx snyk test --severity-threshold=high
```

### 3.3 漏洞处理策略

| CVSS 评分 | 严重级别 | 处理时限 | 处理方式 |
|-----------|---------|---------|---------|
| 9.0 - 10.0 | Critical | 24 小时 | 立即升级，阻断流水线 |
| 7.0 - 8.9 | High | 3 天 | 升级或确认无误报，阻断流水线 |
| 4.0 - 6.9 | Medium | 7 天 | 计划升级 |
| 0.1 - 3.9 | Low | 30 天 | 记录跟踪 |

---

## 4. 镜像扫描（Trivy）

### 4.1 CI 配置

```yaml
trivy-scan:
  stage: security
  image: aquasec/trivy:latest
  script:
    - trivy image
      --severity HIGH,CRITICAL
      --exit-code 1
      --ignore-unfixed
      --no-progress
      $DOCKER_REGISTRY/mitedtsm-server:$CI_COMMIT_SHORT_SHA
  artifacts:
    when: always
    paths:
      - trivy-report.json
    expire_in: 30 days
  only:
    - develop
    - release/*
    - master
```

### 4.2 扫描策略

| 扫描项 | 阻断阈值 | 说明 |
|--------|---------|------|
| OS 包漏洞 | HIGH / CRITICAL | 基础镜像系统漏洞 |
| 应用依赖漏洞 | HIGH / CRITICAL | JAR 包 / Node 包漏洞 |
| 配置错误 | CRITICAL | 危险配置如 root 运行 |
| 密钥泄露 | 任何 | 镜像中硬编码密钥 |

### 4.3 推荐基础镜像

| 镜像 | 推荐版本 |
|------|---------|
| Java 运行时 | `eclipse-temurin:17-jre-alpine` |
| Nginx | `nginx:1.25-alpine` |
| Node.js | `node:20-alpine` |

---

## 5. 密钥管理

### 5.1 密钥管理原则

| 原则 | 说明 |
|------|------|
| **零硬编码** | 严禁在任何代码、配置文件中硬编码密钥 |
| **环境隔离** | 各环境使用独立的密钥集 |
| **最小权限** | 每个密钥仅授予必要的最小权限 |
| **定期轮换** | 密钥每 90 天自动轮换 |
| **审计日志** | 所有密钥访问记录审计日志 |

### 5.2 密钥类型与管理

| 密钥类型 | 存储方式 | 轮换周期 |
|---------|---------|---------|
| 数据库密码 | 环境变量 / K8s Secret | 90 天 |
| Redis 密码 | 环境变量 / K8s Secret | 90 天 |
| JWT 签名密钥 | 环境变量 / K8s Secret | 30 天 |
| 第三方服务密钥（支付/AI/短信） | K8s Secret | 90 天 |
| Docker Registry 密码 | CI/CD 变量 | 90 天 |
| SonarQube Token | CI/CD 变量 | 180 天 |

### 5.3 GitLeaks 密钥扫描配置

```yaml
gitleaks-scan:
  stage: security
  image: zricethezav/gitleaks:latest
  script:
    - gitleaks detect --source="." --verbose --redact
      --report-format=json --report-path=gitleaks-report.json
  artifacts:
    when: always
    paths:
      - gitleaks-report.json
    expire_in: 30 days
  only:
    - branches
```

---

## 6. 安全编码规范

### 6.1 输入验证（SQL 注入防护）

```java
// 正确：使用 MyBatis-Plus 安全方法
customerMapper.selectById(customerId);

// 正确：使用 LambdaQueryWrapper
customerMapper.selectList(new LambdaQueryWrapper<CustomerDO>()
    .eq(CustomerDO::getName, name));

// 错误：直接拼接 SQL 条件
customerMapper.selectList(new LambdaQueryWrapper<CustomerDO>()
    .apply("name = '" + name + "'"));  // SQL注入风险！

// 错误：字符串拼接 SQL
String sql = "SELECT * FROM crm_customer WHERE id = " + customerId;
```

### 6.2 输出编码（XSS 防护）

```vue
<!-- 正确：Vue 自动转义 -->
<div>{{ userInput }}</div>

<!-- 错误：v-html 容易 XSS -->
<div v-html="userInput"></div>

<!-- 正确：必须使用 v-html 时先消毒 -->
<div v-html="$sanitize(userInput)"></div>
```

### 6.3 认证与授权

```java
// 正确：使用 @PreAuthorize 注解权限控制
@PreAuthorize("@ss.hasPermission('crm:customer:create')")
@PostMapping("/create")
public Result<CustomerVO> create(@Valid @RequestBody CustomerDTO dto) {
    return customerService.createCustomer(dto);
}

// 错误：在业务代码中手动检查权限
if (!user.getRoleList().contains("CRM_MANAGER")) { ... }
```

### 6.4 敏感数据处理

```java
// 正确：密码使用 BCrypt 加密
String hashedPassword = BCrypt.hashpw(password, BCrypt.gensalt(12));

// 错误：MD5 存储密码
String hashedPassword = DigestUtils.md5Hex(password);

// 正确：日志中不输出敏感信息
log.info("用户登录成功，用户名: {}", username);

// 错误：日志中输出密码
log.info("用户登录，密码: {}", password);

// 正确：敏感字段脱敏输出
@JsonSerialize(using = PhoneDesensitizeSerializer.class)
private String phone;
```

### 6.5 多租户安全

```java
// 正确：继承 TenantBaseDO 自动注入租户ID
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("crm_customer")
public class CustomerDO extends TenantBaseDO {
    private String name;
}

// 正确：共享表（无租户隔离需求）使用 BaseDO
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("system_dict_type")
public class DictTypeDO extends BaseDO {
    private String name;
}

// 正确：特定场景忽略租户隔离
@TenantIgnore
public List<ConfigDO> getGlobalConfigs() { ... }
```

### 6.6 文件上传安全

```java
// 正确：校验文件类型和大小
if (!ALLOWED_CONTENT_TYPES.contains(file.getContentType())) {
    return Result.error("不支持的文件类型");
}
if (file.getSize() > MAX_FILE_SIZE) {
    return Result.error("文件大小超过限制");
}

// 正确：使用 UUID 重命名，避免路径遍历
String newFileName = UUID.randomUUID().toString() + getExtension(originalName);
fileStorageService.store(file, newFileName);
```

---

## 7. OWASP Top 10 防护矩阵

| 排名 | 漏洞类型 | 防护措施 |
|------|---------|---------|
| A01 | 访问控制失效 | Spring Security + @PreAuthorize + 租户隔离 |
| A02 | 加密失败 | BCrypt (密码) / AES-256-GCM / TLS 1.3 |
| A03 | SQL 注入 | MyBatis-Plus 参数化查询 + 输入校验 |
| A04 | 不安全设计 | 威胁建模 / 安全设计评审 |
| A05 | 安全配置错误 | application.yaml 安全基线 / 配置审计 |
| A06 | 易受攻击组件 | OWASP Dependency-Check / Trivy / pnpm audit |
| A07 | 认证失败 | JWT + Token刷新 + 账户锁定 |
| A08 | 软件和数据完整性 | 签名验证 / 校验和 |
| A09 | 日志和监控失败 | SkyWalking + ELK + 实时告警 |
| A10 | SSRF | URL 白名单 / 内网地址过滤 |

---

## 8. 安全事件响应

### 8.1 事件分级

| 级别 | 定义 | 响应时间 | 处理流程 |
|------|------|---------|---------|
| P0 紧急 | 数据泄露、系统入侵 | 15 分钟 | 立即启动应急响应 |
| P1 高 | 严重漏洞可利用 | 1 小时 | 安全团队紧急修复 |
| P2 中 | 中危漏洞 | 24 小时 | 排期修复 |
| P3 低 | 低危漏洞/配置问题 | 7 天 | 计划修复 |

### 8.2 应急响应流程

```
发现 → 确认 → 隔离 → 修复 → 恢复 → 复盘

1. 发现：安全扫描 / 渗透测试 / 外部报告
2. 确认：验证漏洞真实性、影响范围
3. 隔离：切断受影响系统、下线功能
4. 修复：开发补丁、紧急发版
5. 恢复：验证修复、恢复服务
6. 复盘：Root Cause Analysis、改进措施
```

---

## 9. 安全检查清单

### 9.1 代码审查安全检查

- [ ] 无硬编码密码、密钥、Token
- [ ] 所有用户输入经过校验
- [ ] 所有数据库操作使用 MyBatis-Plus 参数化方法
- [ ] 敏感接口有 @PreAuthorize 权限注解
- [ ] 多租户数据隔离使用 TenantBaseDO 基类
- [ ] 日志输出不包含敏感信息
- [ ] 文件上传有类型和大小限制
- [ ] 异常处理不暴露内部堆栈信息
- [ ] 使用安全的加密算法（BCrypt / AES-256）
- [ ] HTTPS 强制启用
- [ ] 跨域配置最小化（CORS 白名单）

### 9.2 部署前安全检查

- [ ] 镜像扫描通过（无 HIGH/CRITICAL）
- [ ] 依赖扫描通过（无 ≥ 7.0 CVSS）
- [ ] SAST 扫描通过（无 Blocker）
- [ ] 密钥已从镜像中排除
- [ ] 容器以非 root 用户运行
- [ ] 数据库密码已修改为非默认值
- [ ] 资源限制已设置

---

> **文档维护**: 本文档由 DevSecOps 团队和安全负责人联合维护，安全策略变更需经过 Tech Lead 审批。
