# DEPLOY-DATASET-BUG-001：数据集替换由三个配置表达同一意图

- 日期：2026-07-18
- 分支：`develop`
- 状态：已修复

## 问题

已有库数据替换同时依赖策略、允许 cleanup 和持久数据确认三个字段。组合状态多、错误信息绕，且
“replace”本身已经表达清理并替换，却还要求两个布尔值再次确认。

## 修复

- 收敛为 `mysql.dataset_mode: preserve | insert | replace`；
- `preserve` 不执行已有库数据集；
- `insert` 禁止 manifest 出现 cleanup；
- `replace` 强制 manifest 第一项为 cleanup，再按顺序插入；
- 生成器移除清理与持久数据确认字段，只负责生成 SQL/manifest/checksum；
- `deploy.sh` 和低频独立数据集入口共用同一模式语义。

## 验证

运行 YAML 全量 check、数据集正负门禁和临时 MySQL 4 项部署初始化回归通过；新数据集在一次性 MySQL
及本机运行库均按 replace 成功执行，随后配置恢复 preserve。
