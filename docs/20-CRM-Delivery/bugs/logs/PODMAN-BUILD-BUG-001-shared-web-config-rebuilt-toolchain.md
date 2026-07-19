# PODMAN-BUILD-BUG-001：共享 Web 配置误重建工具链

日期：2026-07-18。分支：`develop`。状态：已修复。

## 现象

执行 Web 标准编译配置时没有直接使用已发布的
`ghcr.io/elel-code/group-11-build-ubuntu:26.04`，反而在本机重建工具链镜像，并因 Containerfile
仍引用目录整理前的入口脚本而失败。

## 原因

- `build-web-ubuntu-26.04.yaml` 遗留 `image.rebuild: true`；
- `Containerfile.build-ubuntu` 未随入口脚本迁移到 `podman/internal/` 更新复制路径。

## 修复

- 共享 Web 配置改为 `image.rebuild: false`，普通成员直接使用 public 工具链镜像；
- 更新维护者重建工具链时使用的 Containerfile 路径。

## 回归

使用共享 Web YAML 重新执行编译，并继续通过独立的运行镜像构建与 Web 容器替换阶段验证。
