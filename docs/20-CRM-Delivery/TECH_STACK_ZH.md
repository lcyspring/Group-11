# 技术栈基线与清理边界

更新日期：2026-07-18。分支：`develop`。

本文件记录当前可复现的工程基线。版本的最终真源仍是对应 POM、`package.json`、lockfile、
Containerfile 与运行 KDL；本文用于导航，不在这里复制全部依赖清单。

## 当前基线

| 层级 | 技术与版本 | 真源 |
|---|---|---|
| 后端语言/框架 | Java 17、Spring Boot 3.5.9、Flowable 7.2.0 | `Server/pom.xml`、`Server/mitedtsm-dependencies/pom.xml` |
| 后端构建 | Maven 3.9.12、Surefire 3.5.3、JaCoCo | Ubuntu 26.04 工具链镜像、`Server/pom.xml` |
| 管理端 | Vue 3.5.12、Vite 5.1.4、TypeScript 5.7.3、Element Plus 2.11.1 | `Web/package.json`、`Web/pnpm-lock.yaml` |
| 前端工具链 | Node.js 22.22.1、pnpm 11.3.0 | 公共工具链镜像、`podman/Containerfile.build-ubuntu` |
| 商城 H5 | uni-app Vue 3/Vite、HBuilderX CLI 5.05.2026032412.0052 | HBuilderX 工具链镜像、`MallFrontend/` |
| 数据与中间件 | MySQL 8.0.46、Redis 6.2.22、RabbitMQ 3.13.7、TDengine 3.3.6.0 | `podman/config/runtime-local-check.kdl` |
| 运行与代理 | Temurin 17.0.19、Nginx 1.30.0、rootless Podman | `podman/Containerfile`、`runtime-images.example.kdl` |
| 配置 | KDL、dasel 3.11.2 | `podman/config/`、`podman/tools/install-dasel.sh` |

运行基础镜像除精确标签外还固定 sha256 digest；完整来源、用途与离线策略见
`podman/images/README_ZH.md`。

## 保留的技术边界

- Spring Boot 继续使用原生 `application.yaml`；pnpm 继续使用原生 lock/workspace YAML。
- 项目部署配置只使用 KDL。上述框架原生 YAML 不是旧 Podman 配置，不应机械改名。
- 数据库直接使用官方 MySQL 镜像；仓库 `database/` 只在部署阶段按 manifest 读取，不挂载到长期
  运行容器，也不烘焙到镜像。
- Web 与 Mall 分别封装为 Nginx 镜像，Server 为可执行 JAR；前端不进入 JAR/WAR。

## 已退出日常维护的路径

| 旧路径 | 当前替代 |
|---|---|
| Docker / Docker Compose（遗留 Compose 与 Docker profile 已删除） | rootless Podman |
| Host JDK、Maven、Node、pnpm、HBuilderX 构建 | 两个公开 Ubuntu 26.04 工具链镜像 |
| `up.sh`、`down.sh`、`build-in-ubuntu.sh` 等兼容入口 | `compile.sh`、`build-images.sh`、`deploy.sh`、`stop.sh` |
| 每目标布尔开关 | KDL 的 `include_targets` 白名单与 `exclude_targets` 黑名单 |
| Podman YAML 和自写 YAML 子集解析 | KDL + 固定官方 dasel |
| 数据库 SQL 镜像烘焙/目录扫描 | bootstrap、compatibility、dataset manifest |
| 不支持软链接目录的暂存复制 | 项目目录直接支持 pnpm 正常软链接语义 |

## 后续清理规则

1. 一次只升级一个基础组件或一类前端 API，并保留升级前后可比较的构建与运行证据。
2. 删除依赖前必须证明源码、测试、构建脚本和运行路径均无引用；只看 `package.json` 或 POM 名称不够。
3. 大版本升级必须经过完整编译、模块测试、数据库兼容、启动健康和关键业务人工验收。
4. 不为消除第三方告警直接修改发布镜像或依赖缓存；应升级真源或记录明确的上游限制。
5. 技术栈变化同时更新本文件、对应 ADR、测试覆盖率和 Bug/兼容性日志。
