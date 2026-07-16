# 编译工具链镜像可移植性测试结果

日期：2026-07-17。分支：`develop`。

- 本地镜像 2/2；
- OCI save 2/2，归档共 456 MiB；
- SHA-256/load 2/2；
- Git ignore 命中归档、checksum 和本机 YAML；
- `lcyspring` 命名空间拒绝当前账号创建包；改用已登录的 `elel-code` 命名空间后 push 2/2 成功。
