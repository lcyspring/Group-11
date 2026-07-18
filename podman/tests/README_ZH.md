# Podman 测试资产

- `runtime-config/`：KDL 协议、脚本语法、manifest、目录和无状态门禁；
- `mall-h5-build/`：统一 `compile.sh` 的 H5 容器运行时依赖与断网构建测试；
- `database-deploy-provision/`：使用官方 MySQL 临时容器和卷验证空库初始化、已有库保留与未知库拒绝；
- `marketing-provider-provision/`：隔离库验证短信/邮件 Provider 的 create-only、managed、disabled 和关联完整性；
- `acceptance/`：真实 API/MySQL 业务验收脚本，只在对应服务和 ignored 本机 KDL 就绪后运行。

工单专项入口为 `acceptance/verify-crm-work-order-performance.sh`，共享示例
`config/verify-crm-work-order-performance.example.kdl` 固定要求至少 50 个工单和 50 并发；正式结果写入
CRM evidence 目录，所有临时业务行必须清理为 0。
安全负向入口为 `acceptance/verify-crm-work-order-security.sh`；它创建临时受限角色和用户，覆盖对象
越权、跨租户、非法状态、SQL/XSS 与参数边界，并在退出时删除账号、Token、角色和业务行。

这些脚本不用于普通编译、镜像封装或服务启动。命令行仍只接受一个 KDL 路径。

两个 provision 集成测试需要一个可用的本机运行 KDL。数据库测试自己创建并清理官方 MySQL 容器与
named volume；Provider 测试只复制 System 表结构到临时库并在结束后删除，不覆盖现有账号。
