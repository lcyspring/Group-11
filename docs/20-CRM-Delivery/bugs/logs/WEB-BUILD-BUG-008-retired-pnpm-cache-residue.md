# WEB-BUILD-BUG-008：Deno 依赖卷残留 pnpm 双重目录树

日期：2026-07-19。分支：`develop`。级别：P1/缓存一致性。状态：已关闭。

## 现象与根因

Web 与 Mall 的命名卷中同时存在新的 `.deno` 和旧 `.pnpm`、`.modules.yaml`、workspace state。
`deno install` 会建立新链接，但不会自动删除 pnpm 的历史目录，长期保留会造成磁盘浪费和依赖来源
误判。Mall 集成门禁因此在第 5 项失败。

## 修复关键

- 两个依赖入口只在检测到 `.pnpm` 时清空对应 `node_modules` 缓存卷内容，再由冻结 Deno lock 重建；
- 不删除 Deno package cache，不触碰 Host 源码、构建产物或任何业务数据卷；
- Mall 安装完成后强制断言三类 pnpm 元数据均不存在；结构门禁防止清理逻辑被删除。

## 回归

Web 依赖从 Deno cache 重建并通过 lint；Mall 从缓存重建后完成 H5 编译，依赖卷无 pnpm 元数据，
10/10 集成断言通过。
