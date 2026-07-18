# 数据集集中模式覆盖率

| 范围 | 覆盖 |
|---|---|
| 模式 | preserve / insert / replace，3/3 |
| manifest 正负门禁 | 合法 replace、insert+cleanup、replace 无 cleanup，3/3 |
| 数据库分类 | 空库、完整已有库、未知非空库，3/3 |
| 运行状态保护 | check 前后 Pod 快照一致 |
| 实际数据替换 | 一次性 MySQL 与本机运行库，2/2 |

本项为 Bash/YAML/MySQL 集成覆盖，不计入 Java JaCoCo。
