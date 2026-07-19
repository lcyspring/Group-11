# Deno 与 Vite 8 工具链迁移测试计划

日期：2026-07-19。分支：`develop`。

- 校验 Web/Mall 只保留 `deno.json` 与冻结 `deno.lock`，现行 KDL 和 package script 不再调用 pnpm/Node；
- 校验纯 lint 显式关闭覆盖率后成功，Deno Test 显式开启覆盖率后生成 LCOV 并通过阈值；
- 在 Ubuntu 26.04 公共工具链中完成 Vite 8 production build 和三语分包可达性检查；
- 断网校验离线图标快照覆盖源码、数据库菜单和 IconSelect，产物禁止包含 Iconify API 域名；
- 从带旧 pnpm 元数据的命名卷执行一次迁移，验证只重建依赖缓存；
- 运行 Mall 依赖容器和断网 HBuilderX 构建，检查依赖卷、资产、所有权和 Git ignore；
- 运行 runtime-config 与 documentation 无状态门禁，确认当前 Pod ID 和运行状态不变。
- 对各阶段日志执行 `warning/warn/deprecated` 扫描，命令自身的 `--max-warnings=0` 不计作输出警告。
