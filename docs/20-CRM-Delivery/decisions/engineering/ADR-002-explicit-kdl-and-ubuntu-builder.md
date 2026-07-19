# ADR-002：显式 KDL 配置与 Ubuntu 26.04 工具链

状态：已实施

日期：2026-07-13；修订：2026-07-19

## 决策

1. Podman 编译、镜像封装、部署、停止及运维脚本只读取 KDL；命令行只接受一个 KDL 路径。
2. KDL 是 Podman 流程的唯一配置真源，不接受环境变量、额外命令行参数或旧 YAML 旁路覆盖。
3. 配置由仓库内固定版本的 `dasel` 解析。安装脚本下载官方 GitHub Release、校验架构对应的
   SHA-256，并将二进制写入被忽略的 `podman/tools/bin/`。
4. Server、InitService、Web、测试与覆盖率统一使用公开的 Ubuntu 26.04 工具链镜像；Mall H5 使用
   独立的 Ubuntu 26.04 HBuilderX 无图形工具链镜像。
5. 项目依赖由 Deno 在编译容器运行时下载到 Podman 命名卷；Host 不安装项目 JDK、Maven、Deno、
   Node、npm、pnpm 或 HBuilderX，也不生成项目 `node_modules`。标准镜像不提供 Node 兼容层。
6. 交付固定分为 `compile.sh`、`build-images.sh`、`deploy.sh` 三个互不隐式调用的阶段。数据库 SQL
   不进入应用镜像，而由部署阶段按 manifest 通过 stdin 送入官方 MySQL 容器。
7. 仓库提交字段完整的 `.example.kdl`；真实账号、密码、地址只写入被忽略的 `*-local.kdl`。
8. 不恢复 Docker/Compose、Host 编译和“不支持软链接目录”的兼容入口。

## 理由

KDL 的节点结构比同等规模的扁平环境变量清晰，适合本项目大量分组配置；`dasel` 提供稳定解析和类型
读取，避免继续维护自写 YAML 子集解析器。三阶段拆分使编译工具链、运行镜像和有状态部署各自可验证，
也避免一次脚本调用隐式修改源码产物、镜像和数据卷。

## 验收条件

- 入口参数数量不为 1、字段缺失、重复键、非法层级或非法类型时立即失败；
- 所有 tracked Podman 配置均为 KDL，旧运行 KDL 不再存在；
- 公开工具链镜像可直接 pull，普通成员无需在 Host 重建工具链；
- KDL、Shell、manifest、数据库 provision 和 Pod 无状态门禁持续通过；
- 中文字段参考、镜像来源、三阶段操作、数据保护和故障处置文档同步维护。

## 边界

Spring Boot 的 `application.yaml` 属于框架原生文件，不受本 ADR 的 Podman KDL 唯一入口约束。
Web/Mall 的包管理真源固定为 `deno.json` 与 `deno.lock`；pnpm lock/workspace 已退出且不得恢复。
