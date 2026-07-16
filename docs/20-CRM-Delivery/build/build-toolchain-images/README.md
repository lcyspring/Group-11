# Ubuntu 26.04 编译工具链镜像交付

本机已把两个编译镜像保存为 OCI archive 并完成 checksum/load 回归：

| 镜像 | 本地归档 | 大小 |
|---|---|---:|
| `localhost/mitedtsm-build-ubuntu:26.04` | `mitedtsm-build-ubuntu-26.04.oci.tar` | 309 MiB |
| `localhost/mitedtsm-hbuilderx-ubuntu:26.04-5.05` | `mitedtsm-hbuilderx-ubuntu-26.04-5.05.oci.tar` | 147 MiB |

归档和 SHA-256 位于 ignored 的 `podman/images/`，不会提交 Git。候选 OCI 目标为
`ghcr.io/lcyspring/group-11-build-ubuntu:26.04` 和
`ghcr.io/lcyspring/group-11-hbuilderx-ubuntu:26.04-5.05`。当前宿主未登录 GHCR，因此尚未 push；
登录和仓库权限具备后，把 ignored 本机 YAML 的 `operation.mode` 改为 `push` 即可。
