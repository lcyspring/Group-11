# 旧文档与脚本清理记录

- 分支：`develop`
- 日期：2026-07-19
- 结论：删除与现行容器化交付冲突且无引用的旧说明、裸机部署脚本和生成型临时文件；保留输入分析基线、结构化测试资产与数据库生命周期工具。

## 发现的问题

1. `Web`、`MallFrontend` 和后端遗留 README 仍说明宿主机 Node、npm、pnpm、HBuilderX、旧版 Vite 与上游演示流程，会把成员引向已经退役的技术栈。
2. 后端 Jenkins 与裸机部署脚本硬编码 `/work/projects`、Spring profile、端口和 JVM 参数，绕过 KDL 配置、Ubuntu 26.04 编译容器、镜像封装及 rootless Podman 部署。
3. IDEA HTTP 环境文件保存固定测试 Token；商城目录还提交了三份一次性 i18n 扫描结果，无法作为可重复验证事实源。
4. Shade 构建生成的 `dependency-reduced-pom.xml` 被误提交，清理构建后会反复制造无意义差异。

## 清理结果

- 项目说明只保留根 `README.md` 为统一入口；Podman 的中英文手册继续承担编译、构建、部署和配置说明。
- 删除旧 Jenkins、裸机 Java 部署和 IDEA HTTP 环境文件。日常入口保持为 `podman/compile.sh`、`podman/build-images.sh`、`podman/deploy.sh` 与 `podman/stop.sh`。
- 删除一次性 i18n 清单和已生成 POM，并增加精确忽略规则。
- 文档门禁新增退役文件清单，防止旧说明、硬编码脚本和临时结果重新进入仓库。
- 删除已经完成且无入链的 KDL 迁移计划、结论为零但已失效的旧剩余功能台账，以及仍以 pnpm
  命名卷为验收对象的旧 Mall 构建测试；现行 KDL 契约、剩余工作计划和 Deno/Vite 8 测试继续保留。
- 修正资源安全说明和表格操作测试计划，成员测试只通过 KDL 驱动的 Ubuntu 26.04 容器入口执行。

## 保留边界

- `docs/01-*` 至 `docs/05-*`、`docs/Proj-Docs-v-6/` 是仍被功能说明引用的输入基线，不按“旧”直接删除。
- `podman/tests/` 下的脚本是结构化回归与验收资产，不是日常部署入口，但仍用于验证当前实现。
- `podman/operations/` 下的数据库、镜像、BPM 和诊断脚本具有独立运维职责，未与四个日常入口重复。
- OCI 工具链归档、数据库备份和本机 KDL 属于被忽略的本地资产，本轮不删除有效备份或当前部署配置。

## 验证要求

- 对所有受版本控制的 Shell 脚本执行 `bash -n`。
- 执行文档链接与退役路径门禁。
- 执行 KDL/runtime-config 无状态回归，确认清理未破坏编译、镜像或部署入口。
