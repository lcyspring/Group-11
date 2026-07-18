# PODMAN-DB-BUG-001：无鉴权探针过早接受 MySQL 临时初始化实例

日期：2026-07-18。分支：`develop`。级别：P0。状态：已关闭。

## 现象与根因

官方 MySQL 镜像第一次初始化空卷时会启动临时 Server。原部署探针只执行无密码
`mysqladmin ping`，临时 Server 已可达时便提前通过，但 YAML 配置的 root 密码尚未完成落库；紧接着执行
部署期 bootstrap 会返回 1045。旧的定制 MySQL 镜像把 SQL 放在 entrypoint 内部，掩盖了这个竞态。

## 修复与门禁

正式部署和隔离测试均使用 YAML 的 `health.mysql_user` 与 `mysql.root_password` 执行真实 `SELECT 1`；
`mysqladmin ping` 即使鉴权失败也可能返回 Server 存活，因此不再作为就绪依据。只有目标账号真正可查询后
才开始 manifest。空库测试使用官方 MySQL、临时命名卷且不发布端口，结束后自动清理。
