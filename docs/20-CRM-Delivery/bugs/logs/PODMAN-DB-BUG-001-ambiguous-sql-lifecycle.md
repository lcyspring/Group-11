# PODMAN-DB-BUG-001：数据库脚本生命周期混放且初始化顺序硬编码

## 现象

`database/base` 和 `database/new` 同时容纳完整快照、增量建表、数据回填、种子数据和历史修复；
MySQL 初始化脚本逐个硬编码文件名，兼容迁移又维护第二份路径清单。新增迁移容易漏执行，清理或
销毁脚本也没有独立的安全边界。

## 根因

目录按“旧/新”时间口径增长，没有按 bootstrap、migration、seed、repair、cleanup、teardown 的
执行语义建模；启动流程依赖目录知识而不是显式 manifest。

## 修复

- 完整基线、原子迁移、纯种子、数据修复、人工清理和销毁操作分目录管理；
- 空库初始化改为读取唯一 `mysql-bootstrap.manifest`，不再扫描目录或硬编码文件；
- 运行库兼容迁移使用独立 `mysql-compatibility.manifest`；
- 门禁校验路径不越界、文件存在、全部 CRM 迁移已登记、兼容迁移属于空库初始化子集；
- cleanup 和 teardown 永不允许进入自动清单，seed 禁止结构、清理和销毁语句。

## 验证

静态 manifest 门禁通过；独立空 MySQL 8.0 数据卷完整导入 324 张表。动态验收同时发现并修复
审批孤儿修复脚本对 Flowable 首次启动时序的错误假设：历史表不存在时安全跳过，不会误改审批
数据，也不会阻断新环境初始化。完整结果见 `docs/20-CRM-Delivery/testing/database-lifecycle-governance/`。

## 分支

`develop`
