# 数据库脚本目录

数据库脚本按生命周期分层。运行 KDL 通过 `mysql.sql_root` 显式指定本目录；`deploy.sh` 再由
`internal/provision-database.sh` 读取 KDL 指定的 bootstrap、compatibility 和 dataset manifest。
脚本不扫描目录猜测顺序、不挂载整个目录，也不把 SQL 烘焙进 MySQL 镜像。每个 manifest 条目先确认
仍位于 `mysql.sql_root` 内，再通过 `podman exec -i ... mysql` 的 stdin 顺序发送给 MySQL 容器。

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

运行 KDL 的 `mysql.dataset` 与 `mysql.dataset_manifest` 指定数据集。默认
`mysql.dataset_mode: preserve`，已有卷不会因切换名称而重新导入、清理或覆盖，因此正常
重启和部署保持数据持久化。

已有数据库的数据集行为只由 `mysql.dataset_mode` 声明：`insert` 只允许无 cleanup 的增量数据集；
`replace` 要求 manifest 第一项是 cleanup，并在显式清理范围内完整清除旧数据集后按顺序插入新数据；`preserve` 不执行
数据集。独立
`operations/database/database-dataset.sh` 保留为低频维护和验证入口，不是标准部署主路径。生产替换前
仍应先使用 `database-backup.sh` 备份。

## 演示数据生成与部署隔离

`podman/operations/database/generate-demo-dataset.sh <kdl>` 是独立的离线生成入口，只把固定 seed 和
规模配置渲染为 `database/generated/` 下的 SQL、manifest 与 checksum，不连接 MySQL。生成产物被
Git 忽略，可由相同 KDL 重建。`deploy.sh` 不得调用生成器；它只消费运行 KDL 已明确指定的现成
manifest。默认 `mysql.dataset_mode: preserve`；`insert` 只插入；CRM 演示集的 `replace` 使用
`replacement_cleanup_scope: tenant-crm-demo` 清空租户 CRM/OA 演示业务事实后插入，管理员账号以及
客户公海、合同和工单策略配置保持不变。
生成过程与部署/替换过程始终独立。

当前 CRM 演示数据集还显式生成多用户负责人分布、退款/报销 OWNER 与 INVOLVED 场景、客服处理组、
竞争对手以及 ERP 客户/产品主数据映射。生成器只负责渲染这些事实及部署可消费的 SQL；是否清理旧数据
仍完全取决于部署 KDL 的 `mysql.dataset_mode`。
