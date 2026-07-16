# Ubuntu 26.04 编译工具链镜像交付

本机已把两个编译镜像保存为 OCI archive 并完成 checksum/load 回归：

| 镜像 | 本地归档 | 大小 |
|---|---|---:|
| `localhost/mitedtsm-build-ubuntu:26.04` | `mitedtsm-build-ubuntu-26.04.oci.tar` | 309 MiB |
| `localhost/mitedtsm-hbuilderx-ubuntu:26.04-5.05` | `mitedtsm-hbuilderx-ubuntu-26.04-5.05.oci.tar` | 147 MiB |

归档和 SHA-256 位于 ignored 的 `podman/images/`，不会提交 Git。候选 OCI 目标为
`ghcr.io/elel-code/group-11-build-ubuntu:26.04` 和
`ghcr.io/elel-code/group-11-hbuilderx-ubuntu:26.04-5.05`。2026-07-17 已完成两个镜像 push，成员可执行：

```bash
podman pull ghcr.io/elel-code/group-11-build-ubuntu:26.04
podman pull ghcr.io/elel-code/group-11-hbuilderx-ubuntu:26.04-5.05
```

Server/Web 构建镜像本地摘要为
`sha256:931a2270312ae810d0640764a5e7326dedc3702557f17721e092698a49735c2c`；HBuilderX 构建镜像本地
摘要为 `sha256:37682812a8636ce5e642ece9900bed2e105281698fdf43c1e7dbee115d238b45`。
