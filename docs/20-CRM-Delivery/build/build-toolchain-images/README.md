# Ubuntu 26.04 编译工具链镜像

更新日期：2026-07-18。两个 GHCR package 均为 public，普通成员直接 pull，不需要先构建镜像或登录。

| 用途 | 公共镜像 |
|---|---|
| Server、InitService、Web、测试、覆盖率 | `ghcr.io/elel-code/group-11-build-ubuntu:26.04` |
| Mall H5 无图形编译 | `ghcr.io/elel-code/group-11-hbuilderx-ubuntu:26.04-5.05` |

```bash
podman pull ghcr.io/elel-code/group-11-build-ubuntu:26.04
podman pull ghcr.io/elel-code/group-11-hbuilderx-ubuntu:26.04-5.05
```

项目依赖在容器运行时下载到命名卷，不固化在工具链镜像。只有维护者发布新工具链时才使用
`image.rebuild: true`；日常共享 KDL 必须为 `false`。

## 归档策略

工具链镜像体积大且构建输入特殊，推荐在离线交付时 save。OCI tar 和 SHA-256 写入被 Git 忽略的
`podman/images/`，不提交仓库。归档、校验、加载和上传统一使用
`podman/operations/images/build-image-archives.sh` 与对应 KDL；镜像来源和运行镜像边界见
`podman/images/README_ZH.md`。
