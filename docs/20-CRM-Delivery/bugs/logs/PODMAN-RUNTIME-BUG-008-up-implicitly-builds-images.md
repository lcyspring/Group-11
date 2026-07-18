# PODMAN-RUNTIME-BUG-008：启动脚本隐式封装运行镜像

日期：2026-07-18。分支：`develop`。级别：P1。状态：已关闭。

## 现象与根因

旧 `up.sh` 的 `full` 和 `rebuild-*` 同时执行 `podman build` 与容器替换。编译、镜像封装、部署三个
责任被压进同一个状态操作，导致“替换容器”可能意外消费 Host 上尚未验收的产物，单组件模式名称也
无法判断究竟是构建镜像还是替换服务。

## 修复

- 新增 YAML-only `build-runtime-images.sh`，只封装已有产物；
- `up.sh` 删除全部源码产物检查、Containerfile 读取和 `podman build`；
- 启动模式收敛为 `replace/fast/frontends-only/replace-server/replace-web/replace-mall/check`；
- 基础封装镜像和项目运行镜像分别进入独立 YAML，离线归档名也显式配置；
- 更新中文 README、操作手册、部署指南、配置索引和字段参考。

## 防回归门禁

`up.sh` 中不得出现 `podman build` 或读取 JAR、`dist-prod`、H5 产物的逻辑；镜像封装脚本不得调用
编译入口或 `up.sh`。两个脚本均保持单 YAML 参数契约，并分别提供 `check` 配置。

真实验证已完成五个 target 全量封装、仅 Server 替换和完整 Pod 替换。完整替换后 8080/8081/8082
健康，九个受管 BPM 模型均幂等复用或部署成功。
