# Podman 镜像归档中文说明

本目录保存被 Git 忽略的 OCI tar 和 SHA-256 文件。

- `image-archives.sh`：保存 JDK、MySQL、Redis、RabbitMQ、TDengine、Nginx 等运行基础镜像，主要用于
  离线部署；
- `build-image-archives.sh`：保存/加载/上传 Ubuntu 26.04 Server-Web 与无图形 HBuilderX 编译镜像，
  推荐用于成员环境统一；
- 项目 Server/Web/Mall 运行镜像默认从当前源码产物重建，不建议用长期 tar 代替源码版本管理；
- tar 和 checksum 不提交 Git，需要共享时放入制品库或 OCI 镜像仓库。

候选上传目标由 YAML 显式指定。上传 GHCR 前先由成员本人完成 `podman login ghcr.io`，脚本不会读取
或保存 GitHub Token。
