# Git 分支策略 - 密讯ETM系统 (mitedtsm)

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

## 1. GitFlow 分支模型

本项目采用 **GitFlow** 分支策略，确保代码管理规范、发布流程可控。

```
                    ┌─────────────────────────────────────────────┐
                    │               GitFlow 分支模型                │
                    └─────────────────────────────────────────────┘

master         ★──────●────────────────────────────●──────────●────
                      │\                          /          /
                      │ ●────────────────────────●          /
develop               │/                                    /
                      │                                    /
feature/xxx    ──────●──●──●───────────────────────────────
                          \
hotfix/xxx     ────────────●──●────────────────────────────●──
                                                              \
release/x.x   ────────────────────────────────●──●────────────
```

### 1.1 核心分支（永久性分支）

| 分支名称 | 用途 | 保护级别 | 直接推送 | 生命周期 |
|----------|------|---------|---------|---------|
| `master` | 生产环境代码，仅接受 release/hotfix 合并 | **最高** | 禁止 | 永久 |
| `develop` | 开发主干，所有 feature 分支的汇聚点 | **高** | 禁止 | 永久 |

### 1.2 辅助分支（临时性分支）

| 分支类型 | 命名格式 | 来源 | 目标 | 生命周期 |
|----------|---------|------|------|---------|
| Feature | `feature/*` | develop | develop | 功能完成后删除 |
| Release | `release/*` | develop | master + develop | 发布后删除 |
| Hotfix | `hotfix/*` | master | master + develop | 修复后删除 |
| Bugfix | `bugfix/*` | develop | develop | 修复后删除 |

---

## 2. 分支命名规范

### 2.1 通用格式

```
<type>/<module>-<ticket-id>-<short-description>
```

### 2.2 命名细则

| 前缀 | 用途 | 示例 |
|------|------|------|
| `feature/` | 新功能开发 | `feature/system-ETM-001-user-login` |
| `bugfix/` | 非紧急缺陷修复 | `bugfix/crm-ETM-042-fix-nullpointer` |
| `hotfix/` | 生产紧急修复 | `hotfix/pay-ETM-099-patch-callback` |
| `release/` | 版本发布分支 | `release/v2026.01` |
| `docs/` | 文档变更 | `docs/infra-ETM-077-api-doc-update` |
| `refactor/` | 代码重构 | `refactor/erp-ETM-033-extract-service` |
| `test/` | 测试相关 | `test/bpm-ETM-056-add-integration-test` |
| `chore/` | 构建/工具链 | `chore/server-ETM-061-upgrade-springboot` |

### 2.3 命名规则

- 全部使用**小写字母**
- 单词之间使用**连字符 `-`** 分隔
- 描述部分不超过 30 个字符
- 禁止使用中文、特殊字符、空格
- 模块名使用实际模块简写：`system`, `infra`, `bpm`, `crm`, `erp`, `mall`, `member`, `pay`, `mp`, `ai`, `report`, `unified-product`, `overseas-service`, `wms`, `mes`, `zatca`, `web`, `mobile`, `mall-fe`

### 2.4 版本号规范

采用语义化版本 **SemVer 2.0**：

```
YYYY.MM.PATCH[-PRERELEASE]

示例：
  v2026.01        # 正式发布
  v2026.01-rc.1   # 候选发布
  v2026.01.1      # 补丁版本
```

| 版本号 | 变更类型 | 示例场景 |
|--------|---------|---------|
| MAJOR (YYYY.MM) | 年度/月度主版本 | `2026.01` |
| PATCH | 向后兼容的缺陷修复 | Bug 修复、性能优化 |

---

## 3. 提交规范（Conventional Commits）

### 3.1 提交消息格式

```
<type>(<scope>): <subject>

[optional body]

[optional footer]
```

### 3.2 Type 类型

| Type | 说明 | 示例 |
|------|------|------|
| `feat` | 新功能 | `feat(crm): 添加客户跟进记录功能` |
| `fix` | 缺陷修复 | `fix(pay): 修复支付回调签名验证异常` |
| `docs` | 文档变更 | `docs(api): 更新BPM接口文档` |
| `style` | 代码格式（不影响逻辑） | `style(system): 统一代码缩进` |
| `refactor` | 重构（非功能/修复） | `refactor(common): 提取公共校验方法` |
| `perf` | 性能优化 | `perf(report): 优化报表查询SQL` |
| `test` | 测试相关 | `test(wms): 增加入库服务单元测试` |
| `build` | 构建系统/依赖变更 | `build: 升级Spring Boot至3.5.9` |
| `ci` | CI/CD 配置变更 | `ci: 增加Trivy镜像扫描` |
| `chore` | 其他杂项 | `chore: 更新.gitignore` |
| `revert` | 回滚提交 | `revert: 回滚feat(crm)提交` |

### 3.3 Scope 范围（模块）

| Scope | 说明 |
|-------|------|
| `system` | 系统管理模块（用户/角色/菜单/部门/租户） |
| `infra` | 基础设施模块（文件/配置/代码生成/日志） |
| `bpm` | 工作流模块（Flowable） |
| `crm` | 客户关系管理模块 |
| `erp` | 企业资源计划模块（采购/销售/库存/财务） |
| `mall` | 商城模块（商品/订单/营销） |
| `member` | 会员模块（等级/积分/标签） |
| `pay` | 统一支付模块 |
| `mp` | 微信公众号模块 |
| `ai` | AI/LLM 集成模块 |
| `report` | 报表中心模块 |
| `unified-product` | 统一商品模块 |
| `overseas-service` | 海外服务模块 |
| `wms` | 仓库管理模块 |
| `mes` | 制造执行模块 |
| `zatca` | ZATCA 电子发票模块 |
| `web` | Admin Web 前端 |
| `mobile` | Admin 移动端 |
| `mall-fe` | 商城前端 |
| `server` | 服务端公共/框架 |
| `deploy` | 部署相关 |

### 3.4 提交频率要求

- 每个逻辑单元对应一次提交
- 提交粒度：**可独立回滚**
- 禁止提交包含 WIP（Work In Progress）的代码
- 每日至少提交一次，保证代码不丢失

---

## 4. 合并策略

### 4.1 合并方式

| 合并场景 | 合并方式 | 说明 |
|---------|---------|------|
| feature → develop | **Squash Merge** | 压缩为一个提交，保持 develop 提交历史清晰 |
| release → master | **Merge Commit** | 保留完整提交历史，确保可追溯 |
| release → develop | **Merge Commit** | 同步修复变更 |
| hotfix → master | **Merge Commit** | 保留完整修复记录 |
| hotfix → develop | **Merge Commit** | 同步修复到开发主干 |

### 4.2 合并前置条件

| 条件 | Feature | Release | Hotfix |
|------|---------|---------|--------|
| CI 流水线通过 | 必须 | 必须 | 必须 |
| Code Review 通过 | 至少1人 | 至少2人 | 至少2人 |
| 单元测试覆盖率 ≥ 70% | 必须 | 必须 | 必须 |
| SonarQube 质量门禁 | 必须 | 必须 | 必须 |
| 无合并冲突 | 必须 | 必须 | 必须 |
| 安全扫描通过 | - | 必须 | 必须 |

---

## 5. 保护分支规则

### 5.1 `master` 分支保护规则

- 禁止直接推送（No Direct Push）
- 禁止强制推送（No Force Push）
- 禁止删除分支
- 必须通过 Pull Request 合并
- 必须至少 2 名高级工程师 Approve
- 必须通过全部 CI 检查
- 必须通过安全扫描
- 提交必须签名验证

### 5.2 `develop` 分支保护规则

- 禁止直接推送
- 禁止强制推送
- 禁止删除分支
- 必须通过 Pull Request 合并
- 必须至少 1 名工程师 Approve
- 必须通过全部 CI 检查
- 必须通过 SonarQube 质量门禁

### 5.3 `release/*` 分支保护规则

- 仅允许 Tech Lead 创建
- 禁止强制推送
- 必须通过 Pull Request 合并
- 必须通过全部 CI 检查
- 必须通过安全扫描

---

## 6. Code Review 流程

### 6.1 评审流程

```
┌──────────┐    ┌──────────┐    ┌──────────┐    ┌──────────┐    ┌──────────┐
│  开发者    │───▶│ 创建 PR   │───▶│ CI 自动   │───▶│ 评审者    │───▶│ 合并到    │
│  提交代码  │    │ 分配评审  │    │ 检查通过  │    │ Code     │    │ 目标分支  │
│           │    │          │    │          │    │ Review   │    │          │
└──────────┘    └──────────┘    └──────────┘    └──────────┘    └──────────┘
                     │                │               │               │
                     ▼                ▼               ▼               ▼
              CI 失败则驳回      质量门禁不通过      评审不通过       归档PR
             重新修改提交       阻止合并流程        驳回修改
```

### 6.2 PR 模板

```markdown
## 变更描述
<!-- 简要描述本次变更内容 -->

## 变更类型
- [ ] 新功能 (feat)
- [ ] 缺陷修复 (fix)
- [ ] 重构 (refactor)
- [ ] 文档 (docs)
- [ ] 其他

## 关联工单
ETM-###

## 测试说明
<!-- 描述如何测试本次变更 -->

## 检查清单
- [ ] 代码已通过本地单元测试
- [ ] 新增代码测试覆盖率 ≥ 70%
- [ ] 无遗留的调试代码/日志
- [ ] 已处理空值/边界情况
- [ ] 数据库变更已同步至 InstallPackage/database/
- [ ] API 文档已更新
- [ ] 无安全漏洞引入
- [ ] 多租户数据隔离已验证

## 截图/录屏（如有 UI 变更）
```

### 6.3 评审要点

| 维度 | 要点 | 权重 |
|------|------|------|
| 功能正确性 | 逻辑是否正确，边界是否处理 | 30% |
| 代码质量 | 可读性、可维护性、命名规范 | 20% |
| 安全性 | SQL注入、XSS、权限控制、租户隔离 | 20% |
| 性能 | 数据库查询效率、内存使用 | 15% |
| 测试覆盖 | 单元测试、集成测试 | 10% |
| 文档完整性 | 注释、API文档 | 5% |

### 6.4 评审意见分类

| 级别 | 标记 | 含义 | 处理要求 |
|------|------|------|---------|
| 阻塞 | `BLOCKER` | 必须修复才能合并 | 必须修复 |
| 重要 | `MAJOR` | 建议修复，不阻塞合并 | 强烈建议修复 |
| 建议 | `MINOR` | 优化建议 | 可选 |
| 信息 | `INFO` | 信息性评论 | 无需处理 |

### 6.5 评审时间要求

| PR 类型 | 最大响应时间 | 最大评审周期 |
|---------|------------|------------|
| Hotfix | 30 分钟 | 1 小时 |
| Feature | 4 小时 | 24 小时 |
| Bugfix | 2 小时 | 8 小时 |
| Docs | 8 小时 | 48 小时 |

---

## 7. 分支操作命令速查

```bash
# 从 develop 创建 feature 分支
git checkout develop
git pull origin develop
git checkout -b feature/crm-ETM-001-customer-crud

# 从 develop 创建 release 分支
git checkout develop
git pull origin develop
git checkout -b release/v2026.01

# 从 master 创建 hotfix 分支
git checkout master
git pull origin master
git checkout -b hotfix/pay-ETM-099-patch-callback

# 合并 feature 到 develop（通过 PR Squash Merge）
git checkout feature/crm-ETM-001-customer-crud
git rebase develop
# 通过 PR 页面执行 Squash Merge
# 合并后删除本地分支
git branch -d feature/crm-ETM-001-customer-crud
```

---

## 8. 分支清理策略

| 分支类型 | 清理时机 | 责任人 |
|---------|---------|--------|
| `feature/*` | PR 合并后立即删除 | 开发者 |
| `bugfix/*` | PR 合并后立即删除 | 开发者 |
| `release/*` | 合并到 master + develop 后删除 | Release Manager |
| `hotfix/*` | 合并到 master + develop 后删除 | 修复者 |

---

## 9. 违规处理

| 违规行为 | 处理措施 |
|---------|---------|
| 直接推送到 master/develop | 回滚提交 + 警告 |
| 强制推送保护分支 | 回滚提交 + 严重警告 |
| 跳过 Code Review 合并 | 回滚合并 + 重新评审 |
| 提交包含密钥/密码 | 立即轮换密钥 + 安全培训 |
| 提交包含大型二进制文件 | 回滚提交 + 使用 Git LFS |
| 分支命名不规范 | 要求重命名 + 整改 |

---

> **文档维护**: 本文档由 DevSecOps 团队维护，版本变更需经过 Tech Lead 审批。
