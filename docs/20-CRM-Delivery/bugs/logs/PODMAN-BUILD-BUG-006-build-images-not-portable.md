# PODMAN-BUILD-BUG-006：编译工具链镜像无归档与上传入口

## 现象

运行基础镜像可 save，但 Ubuntu 26.04 Server/Web 和无图形 HBuilderX 编译镜像只能在每位成员本机
重建；后者依赖 HBuilderX 提取，成本高且容易造成版本漂移。

## 修复

增加 YAML-only `check/save/load/push` 入口、OCI archive、SHA-256、覆盖保护和 GHCR 登录/目标仓库
约束。运行镜像仍由源码产物重建，不把大 tar 提交 Git。

## 当前外部状态

Git remote 所有者候选 `ghcr.io/lcyspring/...` 拒绝当前账号创建包；改用已登录账号自己的
`ghcr.io/elel-code/...` 命名空间后两个镜像均上传成功。

## 分支

`develop`
