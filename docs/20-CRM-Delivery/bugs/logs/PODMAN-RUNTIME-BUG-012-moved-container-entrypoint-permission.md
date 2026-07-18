# PODMAN-RUNTIME-BUG-012：移动后的容器入口丢失可执行权限

日期：2026-07-18。分支：`develop`。级别：P1。状态：已关闭。

## 现象与根因

统一 `compile.sh` 已完成 Server、InitService 和 Web 编译，进入 Mall H5 的容器运行时依赖安装时，
`crun` 返回退出码 126：`internal/mall-dependencies-entrypoint.sh` 存在但不可执行。目录整理时文件内容被
完整迁移，但新路径的 Unix 可执行位未被保留；仅执行 `bash -n` 只能验证语法，无法发现容器直接把脚本
作为 entrypoint 启动时的权限问题。

## 修复

- 为三个内部容器入口统一设置可执行权限；
- 保持 H5 依赖安装发生在工具链容器运行时，未退回 Host 或镜像构建阶段；
- 继续由统一 `compile.sh` 调用内部入口，不新增面向成员的独立 H5 脚本。

## 防回归门禁

运行配置测试除语法检查外，显式检查 `internal/` 下三个容器入口均为可执行文件。四目标完整编译必须
实际进入 Mall 依赖容器及断网 HBuilderX 容器，避免只验证 Server/Web 后误报成功。修复后同一
`compile.sh` 已连续完成全部四个目标，H5 结构化验收 10/10 通过。
