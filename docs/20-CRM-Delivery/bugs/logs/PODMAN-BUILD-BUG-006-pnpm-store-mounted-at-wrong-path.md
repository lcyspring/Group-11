# PODMAN-BUILD-BUG-006：pnpm store 命名卷挂载路径错误

更新日期：2026-07-14

## 现象

Web 容器构建成功后，项目根目录出现未跟踪的 `.pnpm-store/`，大小约 1.1 GB；安装日志显示 1094 个包全部下载、缓存复用数为 0。

## 根因

Podman 将缓存卷挂载到 `/root/.local/share/pnpm/store`，但 pnpm 11 为保证 store 与项目位于同一文件系统，实际选择 `/workspace/.pnpm-store/v11`。配置的命名卷没有承载真实缓存，反而由项目根绑定挂载把缓存写回宿主。

## 修复

- YAML 新增 `cache.pnpm_store_path: /pnpm-store`。
- Podman 把 `cache.pnpm_store_volume` 挂载到该显式容器路径。
- pnpm install 显式使用同一路径，且脚本拒绝 `/` 或 `/workspace` 下的 store 路径。
- `.gitignore` 增加 `.pnpm-store/` 作为构建缓存防护；本轮误生成目录清理后不作为源码保留。

## 验证

清空 Web `node_modules` 测试卷后，以 Web-only Ubuntu 26.04 配置复跑：1094 个包安装成功，pnpm 命名卷占用 1.1 GB，Vite 生产构建成功，宿主未再出现 `.pnpm-store/`。

## 状态

已关闭。
