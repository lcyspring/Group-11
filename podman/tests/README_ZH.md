# Podman 测试资产

- `runtime-config/`：YAML 协议、脚本语法、manifest、目录和无状态门禁；
- `mall-h5-build/`：统一 `compile.sh` 的 H5 容器运行时依赖与断网构建测试；
- `database-deploy-provision/`：使用官方 MySQL 临时容器和卷验证空库初始化、已有库保留与未知库拒绝；
- `marketing-provider-provision/`：隔离库验证短信/邮件 Provider 的 create-only、managed、disabled 和关联完整性；
- `acceptance/`：真实 API/MySQL 业务验收脚本，只在对应服务和 ignored 本机 YAML 就绪后运行。

这些脚本不用于普通编译、镜像封装或服务启动。命令行仍只接受一个 YAML 路径。

两个 provision 集成测试需要一个可用的本机运行 YAML。数据库测试自己创建并清理官方 MySQL 容器与
named volume；Provider 测试只复制 System 表结构到临时库并在结束后删除，不覆盖现有账号。
