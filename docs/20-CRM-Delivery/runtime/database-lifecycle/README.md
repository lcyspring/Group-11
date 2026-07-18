# MySQL 脚本生命周期与执行边界

空数据卷由 MySQL 镜像入口读取 `database/manifests/mysql-bootstrap.manifest`。已有数据卷在每次部署
前由 `deploy.sh` 读取运行 YAML 的 `mysql.compatibility_migration_manifest`，当前指向
`database/manifests/mysql-compatibility.manifest`。

两条链路只共享幂等增量迁移，不共享执行时机：bootstrap 可加载上游完整快照，compatibility 绝不
重放含 DROP 的基线；cleanup 与 teardown 不进入任何自动链。新增 SQL 时必须先确定生命周期，
再登记对应 manifest，并通过 runtime-config 门禁。
