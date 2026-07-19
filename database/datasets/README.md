# 可选数据集

每个数据集使用 manifest 明确列出 SQL 文件。运行配置用 `mysql.dataset` 标识名称，并用
`mysql.dataset_manifest` 显式指定路径：

- `legacy-demo-v1`：保留上游基线自带的 CRM 演示数据，适合本地展示和验收；
- `none`：在空卷基线导入后清除上游 CRM 演示业务数据，仅保留首次登录和必要基础配置。

已有持久卷在默认 `mysql.dataset_mode: preserve` 下不会切换。部署期可选择 `insert` 或 `replace`；
`replace` 强制 manifest 第一项为 cleanup，随后按顺序插入。独立 `database-dataset.sh` 仅用于维护/验证。后续新增数据包应把
清理与插入顺序写入自己的 manifest，不得依赖目录扫描；离线生成器与部署保持分离。
