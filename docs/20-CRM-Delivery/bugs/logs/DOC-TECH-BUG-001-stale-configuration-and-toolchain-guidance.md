# DOC-TECH-BUG-001：旧配置与工具链说明继续误导现行交付

日期：2026-07-18。分支：`develop`。级别：P1/工程可维护性。状态：已关闭。

## 现象

当前交付目录仍保留旧 YAML 运行观察、重复 OA 测试目录、失效构建脚本路径、已删除测试树链接和
`full/rebuild-*` 模式说明。代码树同时残留未被引用的 Docker Compose、Spring Docker profile 以及
Host `node_modules`/pnpm store。成员按这些文档操作会绕过 KDL 三阶段流程或得到不存在的入口。

## 根因

配置从 YAML 迁移到 KDL、脚本重命名和数据库部署边界收敛分多批完成，功能/测试文档只在各自开发
时点更新，没有统一的文档生命周期、相对链接和旧入口防回归门禁。历史运行观察与现行操作说明也未
分层。

## 修复

- 删除旧 YAML 运行观察目录和重复 `tests/oa-loan`；
- 清理本机 ignored 且已被 `20-CRM-Delivery` 替代的 `docs/06`～`18` 重复文档树；
- 重写技术栈基线、文档治理、工程 ADR、构建说明、三阶段和核心导航；
- 现行命令统一到 KDL，并区分 Spring/pnpm 原生 YAML；
- 删除未引用的 Compose 与 Spring Docker profile；清理 Host 前端依赖残留；
- 新增只读文档门禁，检查 Markdown 链接、绝对路径、旧 YAML、旧启动模式和已删除目录；
- 历史 Bug 日志保留原始术语，不用批量替换改写事实。

## 回归

- 全部 Podman Shell 通过 `bash -n`；
- 文档门禁：56 个相对链接，失效 0，旧入口 0；
- runtime-config KDL、manifest、数据库/Provider/BPM preflight 全部通过；
- 检查前后 Pod ID 不变，`mitedtsm-rootless` 保持 Running；
- `git diff --check` 通过。

## 遗留边界

Spring `application.yaml`、测试 YAML、pnpm lock/workspace YAML 是框架原生真源，不能为追求扩展名
统一而删除。后续依赖升级和 Sass `@import` 清理继续按技术栈 Plan 独立推进。
