# PODMAN-BUILD-BUG-006：编译工具链镜像无归档与上传入口

## 现象

运行基础镜像可 save，但 Ubuntu 26.04 Server/Web 和无图形 HBuilderX 编译镜像只能在每位成员本机
重建；后者依赖 HBuilderX 提取，成本高且容易造成版本漂移。

## 修复

增加 YAML-only `check/save/load/push` 入口、OCI archive、SHA-256、覆盖保护和 GHCR 登录/目标仓库
约束。运行镜像仍由源码产物重建，不把大 tar 提交 Git。

## 当前外部状态

候选目标为 `ghcr.io/lcyspring/...`；当前宿主尚未登录 GHCR，因此上传尚未执行。

## 分支

`develop`
