# Podman 三阶段交付流程

日期：2026-07-18。分支：`develop`。状态：已完成并通过真实替换验证。

后续交付固定拆为三个互不隐式调用的阶段，所有命令行只接收一个 YAML 路径：

1. 编译产物：`build-in-ubuntu.sh` 与 `build-mall-h5-in-ubuntu.sh`；
2. 封装运行镜像：`build-runtime-images.sh`；
3. 启动或替换容器：`up.sh`。

阶段一使用 `ghcr.io/elel-code` 下两个公开 Ubuntu 26.04 工具链镜像，依赖由容器运行时下载或复用
named volume，Host 不安装项目依赖。阶段二只读取已经生成的 JAR、Web、Mall H5 和数据库文件，
不运行 Maven、pnpm 或 HBuilderX。阶段三只消费已经存在、可从归档加载或可从仓库拉取的运行镜像，
不读取源码产物且不执行 `podman build`。

标准入口：

```bash
bash podman/build-in-ubuntu.sh podman/config/build-ubuntu-26.04.yaml
bash podman/build-mall-h5-in-ubuntu.sh podman/config/build-mall-h5-ubuntu-26.04.yaml
bash podman/build-runtime-images.sh podman/config/runtime-images.example.yaml
bash podman/up.sh podman/config/runtime-local.yaml
```

单组件更新也必须先封装镜像，再使用 `replace-server`、`replace-web` 或 `replace-mall`。模式名描述的
只是容器替换行为，不再同时承担编译或镜像构建职责。
