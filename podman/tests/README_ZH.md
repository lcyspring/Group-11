# Podman 测试资产

- `runtime-config/`：YAML 协议、脚本语法、manifest、目录和无状态门禁；
- `mall-h5-build/`：统一 `compile.sh` 的 H5 容器运行时依赖与断网构建测试；
- `acceptance/`：真实 API/MySQL 业务验收脚本，只在对应服务和 ignored 本机 YAML 就绪后运行。

这些脚本不用于普通编译、镜像封装或服务启动。命令行仍只接受一个 YAML 路径。
