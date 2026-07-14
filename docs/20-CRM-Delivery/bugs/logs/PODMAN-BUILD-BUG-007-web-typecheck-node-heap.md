# PODMAN-BUILD-BUG-007：Vue 全量类型检查受 Node 默认堆限制中止

- 发现/关闭日期：2026-07-14
- 级别：P1 / 构建可重复性

## 现象与根因

Ubuntu 26.04 容器执行 `pnpm ts:check` 时，容器仍有可用内存，但 Node 在约 2GB 默认 V8 堆上
触发 `Reached heap limit` 并以 134 退出。原脚本直接调用 `vue-tsc --noEmit`，没有像生产 Vite
构建一样显式配置堆上限，因此大型仓库的类型检查不可重复。

## 修复与验证

`ts:check` 改为通过 Node 启动 `vue-tsc`，显式设置 `--max_old_space_size=6144`；同时新增
`podman/config/check-web-types-ubuntu-26.04.yaml`，类型检查仍只通过 YAML 配置文件启动。
6GB 堆下检查器不再 OOM，能够完整输出类型诊断；其发现的全仓历史错误另记
`WEB-TYPE-BUG-001`，不与内存缺陷混淆。
