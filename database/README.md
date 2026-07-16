# 数据库脚本目录

数据库脚本按生命周期分层，自动启动只读取 `manifests/mysql-bootstrap.manifest` 和运行 YAML 指定的
兼容迁移清单，不再扫描目录猜测顺序。

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
