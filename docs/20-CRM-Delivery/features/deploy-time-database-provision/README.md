# CRM-FEATURE-062：部署期数据库显式 provision

## 目标

数据库 SQL 与运行镜像解耦。MySQL 直接使用运行 YAML 指定的官方镜像，建表、数据集和兼容迁移在
部署阶段从仓库通过 stdin 发送，不复制进镜像，也不挂载到长期运行容器。

## 配置契约

```yaml
image:
  mysql_base: docker.io/library/mysql:8.0

mysql:
  dataset: legacy-demo-v1
  bootstrap_policy: initialize-empty
  bootstrap_manifest: ../../database/manifests/mysql-bootstrap.manifest
  compatibility_migration_manifest: ../../database/manifests/mysql-compatibility.manifest
```

- `initialize-empty`：只对确认无表的库执行 bootstrap 和选定数据集；
- `require-existing`：空库直接失败，适用于禁止自动建立基线的环境；
- 已存在 `system_users`：保留业务数据，跳过 bootstrap 和数据集，仅重放幂等兼容清单；
- 非空但缺少 `system_users`：视为未知或不完整库，拒绝破坏性覆盖。

已有库的数据集替换不属于普通部署，必须使用独立运维入口和双确认。整套持久卷重建也必须在停止
配置中同时开启 `remove_volumes_on_down` 与 `confirm_persistent_data_reset`。

## 实现入口

- `podman/internal/provision-database.sh`：部署期分类、清单校验和 SQL 执行；
- `podman/deploy.sh`：在 MySQL 真实鉴权探针成功后调用 provision；
- `database/manifests/`：bootstrap、compatibility 和 dataset 的唯一显式顺序；
- `podman/tests/database-deploy-provision/run.sh`：使用临时官方 MySQL 容器和临时卷执行隔离回归。

## 安全边界

- manifest 只能引用 `database/` 内文件，禁止绝对路径和目录逃逸；
- bootstrap/compatibility 禁止 cleanup，全部自动清单禁止 teardown；
- MySQL 就绪以带 YAML 凭据的 `SELECT 1` 判断，不能使用鉴权失败仍返回存活的探针；
- SQL 文件不会被写入镜像层或长期运行容器文件系统；
- 普通 `replace` 保留 named volume，不以部署为理由自动替换演示数据。

## 当前结论

已完成。空库初始化、已有库保留、未知库拒绝和 `require-existing` 拒绝空库四条关键路径均已在隔离
官方 MySQL 上通过；真实本地部署已识别并保留 388 张现有表，仅重放幂等兼容清单。
