# MySQL 脚本生命周期与执行边界

确认空库后，由 `deploy.sh` 的部署期 provision 从仓库读取
`database/manifests/mysql-bootstrap.manifest`，通过 stdin 发送给官方 MySQL 容器。已有完整数据库在
每次部署前读取运行 KDL 的 `mysql.compatibility_migration_manifest`，当前指向
`database/manifests/mysql-compatibility.manifest`。

两条链路只共享幂等增量迁移，不共享执行时机：bootstrap 可加载上游完整快照，compatibility 绝不
重放含 DROP 的基线；cleanup 与 teardown 不进入任何自动链。新增 SQL 时必须先确定生命周期，
再登记对应 manifest，并通过 runtime-config 门禁。

数据库目录不进入应用运行镜像，也不挂载到长期运行容器。`initialize-empty` 只处理无表数据库；
`require-existing` 禁止空库初始化；非空但缺少 `system_users` 标记的未知库会被拒绝，避免破坏性覆盖。
