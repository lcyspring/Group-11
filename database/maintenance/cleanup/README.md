# 数据清理

这里放置 DELETE 等清理脚本。清理脚本必须写明影响范围、备份或恢复方案，且永不加入 bootstrap
或 compatibility 清单。

`cleanup-upstream-crm-demo.sql` 仅由 `none` 数据集显式引用：空卷初始化时清除上游演示数据；已有
持久卷只有在 `database-dataset.sh` 的 YAML 明确允许清理和修改持久数据时才可执行。普通部署不会
调用该脚本。
