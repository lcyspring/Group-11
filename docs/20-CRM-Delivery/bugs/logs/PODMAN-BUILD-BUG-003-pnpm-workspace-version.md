# PODMAN-BUILD-BUG-003：pnpm 主版本与仓库配置不兼容

更新日期：2026-07-14

## 现象

Server 和 InitService 构建成功后，Web 依赖安装立即失败：

```text
ERROR packages field missing or empty
```

## 根因

专用镜像固定 pnpm 9.15.4，但仓库 `Web/pnpm-workspace.yaml` 使用 pnpm 11 的 `allowBuilds` 生命周期脚本白名单。pnpm 9 把该文件当作缺失 `packages` 的旧式 workspace 配置并拒绝安装。宿主当前使用 pnpm 11.3.0，因此此前宿主构建没有暴露该偏差。

## 修复

- 将显式 YAML 的 `toolchain.pnpm_version` 改为 11.3.0。
- 同步更新 Containerfile 和脚本默认值，保持无配置回退行为一致。
- 仍由 YAML 控制实际版本，命令行不新增版本参数。

## 验证

Ubuntu 26.04 完整容器复跑确认 pnpm 11.3.0、frozen-lockfile 安装和 `build:prod` 均成功。

## 状态

已关闭。
