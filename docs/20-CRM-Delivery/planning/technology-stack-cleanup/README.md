# 技术栈清理 Plan

制定日期：2026-07-18。实施分支：`develop`。状态：基础设施收敛完成，依赖/API 清理持续推进。

## 已完成

| 项目 | 结果 |
|---|---|
| 运行编排 | 退出 Docker/Compose，统一 rootless Podman |
| 配置入口 | 退出 Podman YAML 与自写解析，统一 KDL + dasel 3.11.2 |
| Host 工具链 | 退出 Host JDK/Node/pnpm/HBuilderX 和无软链接兼容路径 |
| 构建工具链 | 两个公开 Ubuntu 26.04 镜像统一 Server/Web/测试与 Mall H5 |
| 交付阶段 | `compile.sh`、`build-images.sh`、`deploy.sh` 完全分离 |
| 数据库镜像 | 直接运行官方 MySQL；SQL 由部署期 manifest + stdin provision |
| 运行镜像 | Temurin、Nginx、MySQL、Redis、RabbitMQ、TDengine 精确版本与 digest 固定 |
| 前端告警第一阶段 | Web Vite 原生 ESM；Mall 自有 Sass 旧全局函数告警清零 |

当前完整版本和真源见 [技术栈基线](../../TECH_STACK_ZH.md)，操作入口见
[Podman 操作手册](../../../../podman/OPERATIONS_ZH.md)。

## 后续工作

### P1：仓库自有前端 API

- 迁移 Mall 源码和可维护 `uni_modules` 中的 Sass `@import`；
- 保持 Web `vue-tsc`、ESLint、生产构建和 Mall H5 构建通过；
- HBuilderX 内置 legacy JS API/Browserslist 告警等待可复现工具链升级，不热改公共镜像。

### P1：直接依赖审计

- 以源码导入、插件配置、运行加载和测试引用四类证据确认未使用依赖；
- Java BOM 中的兼容数据库、支付、IoT 等可选依赖不能因当前 CRM 未调用就直接删除；
- 每批删除后运行对应 reactor、Web 类型检查和生产构建。

### P2：基础组件升级

| 候选 | 主要风险 | 前置门禁 |
|---|---|---|
| Redis 6.2 后续受支持版本 | 数据格式、命令和 Lua 行为 | 备份恢复、缓存、锁与调度回归 |
| JDK 17 后续 LTS | Flowable、MyBatis、插件与反射 | 全 reactor、CRM/BPM/Infra、启动与内存基线 |
| MySQL 8.0 后续线 | 排序规则、认证、保留字、升级路径 | 隔离恢复、全 manifest、财务与统计对账 |
| HBuilderX 后续版本 | uni-app 编译器、Sass、插件兼容 | H5 完整编译、静态资源和核心商城流程 |

## 完成定义

- 无旧 Podman YAML、旧日常脚本、Host 项目依赖或隐式环境变量覆盖；
- 每项依赖删除/升级都有独立测试结果和 Bug/兼容性记录；
- KDL 示例、技术栈基线、镜像来源和覆盖率数据与源码一致；
- 数据库升级通过备份、隔离恢复、已有卷兼容和持久化验证；
- 不用“消除告警”替代业务正确性与可维护性验收。
