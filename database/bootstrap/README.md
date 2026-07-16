# Bootstrap 基线

这里保存上游模块的完整 MySQL 快照。快照通常同时包含 `DROP TABLE IF EXISTS`、`CREATE TABLE` 和
必要基础数据，必须整体按清单顺序导入空库，不应对运行中的数据库手工执行。
