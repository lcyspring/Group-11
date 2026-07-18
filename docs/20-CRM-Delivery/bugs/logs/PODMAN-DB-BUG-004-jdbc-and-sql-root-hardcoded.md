# PODMAN-DB-BUG-004：JDBC URL 与数据库脚本根目录未完全显式配置

## 发现

部署 KDL 可以修改数据库名、密码和数据集，但 Server 的 local profile 仍固定连接
`127.0.0.1:3306/mitedtsm_database`；数据库 provision 也由脚本固定拼接仓库 `database/`。

## 根因

早期容器部署只覆盖了 Spring 数据源用户名和密码，没有覆盖 URL。SQL manifest 虽然显式配置，但用于
约束 manifest 条目的生命周期根目录仍来自脚本目录推导。

## 影响

- 修改 `mysql.database` 后，MySQL 容器和 provision 使用新库名，Server 却仍连接旧库；
- MySQL 地址、端口和 JDBC 参数不能按环境调整；
- `database/` 来源没有完整出现在配置审计中；
- 管理凭据、应用凭据和健康检查账号职责混杂。

## 修复

- KDL 新增 `mysql.sql_root/host/port/administration_username/application_username/application_password/jdbc_parameters`；
- `deploy.sh` 组合并注入完整 JDBC URL，MySQL 内部端口也由同一字段控制；
- local profile 只接受显式环境变量，不再保存 master/slave 固定连接串；
- provision 从 `mysql.sql_root` 读取生命周期根目录，三个 manifest 及全部条目必须位于该根目录内；
- SQL 仍通过 stdin 发送到 MySQL，不挂载目录、不扫描目录、不进入运行镜像。

## 回归

KDL 结构、Shell 语法、显式字段、JDBC 注入和 SQL 根目录边界均进入 runtime-config 门禁；完整无状态
部署预检通过且 Pod 保持 Running。隔离 MySQL 的空库、已有库、未知库和 `require-existing` 路径 4/4
通过，营销 Provider 管理凭据回归 4/4 通过，临时容器和卷均已清理。
