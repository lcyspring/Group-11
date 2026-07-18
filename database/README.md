# 数据库脚本目录

数据库脚本按生命周期分层。`deploy.sh` 通过 `internal/provision-database.sh` 读取
`manifests/mysql-bootstrap.manifest` 和运行 YAML 指定的兼容迁移清单，不再扫描目录猜测顺序，也不把
SQL 烘焙进 MySQL 镜像。

| 目录 | 用途 | 自动执行 |
|---|---|---|
| `bootstrap/` | 上游完整基线快照，包含建表和必要基础数据 | 仅空库初始化 |
| `migrations/` | 不可拆分的增量变更，允许同一事务意图内包含 DDL、回填和权限种子 | 空库初始化及兼容迁移清单 |
| `seed/` | 只插入可重复构造的数据 | 按 bootstrap 清单显式执行 |
| `maintenance/repair/` | 修复已有错误数据或配置 | 仅兼容迁移清单显式执行 |
| `maintenance/cleanup/` | 清理临时、过期或测试数据 | 永不自动执行 |
| `teardown/` | DROP/TRUNCATE 等销毁操作 | 永不自动执行 |
| `manifests/` | 唯一执行顺序 | 由初始化或部署脚本读取 |

`bootstrap/` 保留上游 dump 的原子性，其中的 `DROP TABLE IF EXISTS` 只用于新库导入，不代表可在
运行库执行。新增功能不得再使用 `base/`、`new/` 这类含义不清的目录，也不得把清理或销毁脚本加入
自动清单。

## 数据集与持久化

运行 YAML 的 `mysql.dataset` 仅供部署脚本在确认空库后选择初始数据集。已有卷不会因
切换该字段而重新导入、清理或覆盖，因此正常重启和部署保持数据持久化。

已有数据库需要替换数据集时，只能使用 `podman/operations/database/database-dataset.sh <yaml>`。配置必须显式声明：

- `mysql.cleanup_existing_before_dataset`：是否允许执行数据集内的 cleanup SQL；
- `mysql.confirm_persistent_data_change`：在 `replace` 模式下确认修改持久数据。

清理开关与 manifest 内容不一致，或持久数据确认未开启时，脚本拒绝执行。生产操作前仍应先使用
`database-backup.sh` 备份；数据集替换不是普通启动步骤。
