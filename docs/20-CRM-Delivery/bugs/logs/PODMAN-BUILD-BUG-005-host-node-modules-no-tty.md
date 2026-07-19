# PODMAN-BUILD-BUG-005：容器复用宿主 node_modules 且无 TTY

更新日期：2026-07-14

## 现象

pnpm 11 版本对齐后，Web 安装仍在容器中失败：

```text
ERR_PNPM_ABORTED_REMOVE_MODULES_DIR_NO_TTY
Aborted removal of modules directory due to no TTY
```

## 根因

项目根目录整体绑定到 `/workspace`，使容器直接看到宿主 pnpm 创建的 `Web/node_modules`。容器的 pnpm store 路径与宿主不同，需要重建依赖链接；非交互环境没有 TTY，pnpm 为避免意外删除而中止。

这同时暴露出构建隔离缺口：即使工具链在 Ubuntu 容器内，直接使用宿主 `node_modules` 仍会让结果受宿主依赖状态影响。

## 修复

- YAML 增加 `cache.web_node_modules_volume`，Podman 将该命名卷挂载到 `/workspace/Web/node_modules`。
- YAML 增加 `build.ci: true`，容器仅在该配置开启时显式传入 `CI=true`。
- pnpm store 与项目 `node_modules` 分别使用独立命名卷；宿主目录不再参与容器依赖链接。

## 验证

完整容器复跑在独立命名卷安装 1094 个包，frozen-lockfile 和 `build:prod` 成功；宿主 `node_modules` 未被挂载到容器目标路径。

## 状态

已关闭。
