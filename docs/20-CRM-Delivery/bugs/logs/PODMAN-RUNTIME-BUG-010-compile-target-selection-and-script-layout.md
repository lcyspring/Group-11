# PODMAN-RUNTIME-BUG-010：编译选择仍按引擎白名单且脚本混放

日期：2026-07-18。分支：`develop`。级别：P1。状态：已关闭。

## 现象与根因

统一入口初版仍通过 `build.engine` 在 standard/HBuilderX 二选一，无法用一份 YAML 同时选择
Server、InitService、Web 和 Mall H5，也不能表达“默认全选但排除某项”。同时真实 API 验收、数据库
维护、镜像归档、BPM provision 和诊断脚本全部混在 `podman/` 根目录，普通成员难以识别日常入口。

## 修复

- H5 依赖安装和断网编译逻辑直接并入 `compile.sh`，删除独立 H5 引擎脚本；
- `build.include_targets` 提供白名单，`build.exclude_targets` 提供优先级更高的黑名单；
- 目标固定为 `server,init-service,web,mall-h5`，同时支持 `all` 和 `none`；
- 新增四目标完整示例，59 份既有编译/测试 YAML 从产物布尔值迁移到目标集合；
- 根目录只保留四个日常入口；验收脚本进入 `tests/acceptance/`，低频维护进入 `operations/`，
  容器入口进入 `internal/`。

## 防回归门禁

配置测试检查所有编译 YAML 同时声明 include/exclude，拒绝未知、重复或空工作选择；H5 集成测试验证
统一入口确实执行容器运行时依赖安装和断网构建。目录门禁确认根目录不再出现 `verify-*` 或数据库、
镜像、BPM、诊断维护脚本。
