# PODMAN-BUILD-BUG-001：容器错误继承宿主回环代理

更新日期：2026-07-14

## 现象

YAML 已配置 `network.use_host_proxy: false`，但 Ubuntu 镜像的 `apt-get update` 仍尝试连接宿主代理 `127.0.0.1:20122`。容器内的 `127.0.0.1` 指向容器自身，连接失败，导致工具链镜像无法构建。

## 根因

Podman 默认向 build/run 容器传递宿主代理环境。仅不显式添加 `--env` 并不能关闭该继承行为；同时，启用代理时直接传入宿主回环地址也无法从容器访问宿主代理。

## 修复

- YAML 关闭宿主代理时，`podman build` 与 `podman run` 均显式传入 `--http-proxy=false`。
- 容器入口执行 Maven/pnpm 时清除所有大小写代理环境变量，避免工具二次继承。
- YAML 开启宿主代理时，将 URL 中的 `127.0.0.1` 或 `localhost` 转换为 `host.containers.internal` 后显式传入。

## 验证

- 关闭代理配置后，Ubuntu 工具链镜像成功完成 `apt-get update` 和软件安装。
- 完整构建已进入 Maven 多模块编译阶段，不再出现回环代理连接错误。

## 状态

已关闭；Ubuntu 完整构建通过。
