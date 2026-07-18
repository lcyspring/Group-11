# 可选数据集

每个数据集使用 `<name>.manifest` 明确列出 SQL 文件。运行配置的 `mysql.dataset` 仅在新建空 MySQL
数据卷时应用：

- `legacy-demo-v1`：保留上游基线自带的 CRM 演示数据，适合本地展示和验收；
- `none`：在空卷基线导入后清除上游 CRM 演示业务数据，仅保留首次登录和必要基础配置。

已有持久卷不会因为修改 `mysql.dataset` 而自动切换。替换已有库必须使用
`podman/operations/database/database-dataset.sh`，并在独立 YAML 中显式确认是否执行 cleanup。后续新增数据包应把清理与
插入顺序写入自己的 manifest，不得依赖目录扫描。
