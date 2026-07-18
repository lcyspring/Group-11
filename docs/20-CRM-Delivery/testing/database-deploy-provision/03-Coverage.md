# 覆盖率：部署期数据库 provision

本项是 Bash/Podman/MySQL 集成入口，不进入 Java JaCoCo；覆盖率按可观察状态分支记录，不与 CRM Java
行覆盖率混算。

| 覆盖对象 | 覆盖 |
|---|---|
| 核心验收场景 | 4/4 |
| 数据库分类 | 空库、完整已有库、非空未知库，3/3 |
| bootstrap policy | `initialize-empty`、`require-existing`，2/2 |
| 数据集关键断言 | `none` 清除 CRM 演示客户，1/1 |
| 持久化保护 | 已有库保留、未知库拒绝，2/2 |

未计入：离线 archive 拉取失败、真实生产数据量性能和所有 manifest 路径攻击组合；这些由
runtime-config 静态门禁、备份恢复演练及目标环境验收分别承担。
