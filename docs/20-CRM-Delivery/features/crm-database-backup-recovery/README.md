# CRM 数据备份与恢复

当前 CRM 业务真源和受保护附件使用 MySQL 数据库文件客户端；Redis 是缓存、RabbitMQ 是消息链，
TDengine 属于 IoT 范围。因此 CRM 恢复基线使用 MySQL `single-transaction` 一致性 dump：

- gzip 原子写入，不留下半成品正式文件；
- 同名 SHA-256 文件，恢复前同时校验 checksum 和 gzip；
- 默认禁止覆盖已有归档、已有目标库和正在运行的真源库；
- 隔离库恢复后检查七张 CRM 核心表和总表数；
- 演练配置可验证后自动删除隔离库。

命令行仍只接受 KDL 路径，密码、归档位置和所有破坏性授权均在 ignored 本机配置中显式声明。
