# 数据库初始化数据集测试计划

1. 验证 `mysql.dataset` 必填、名称安全且 manifest 存在；
2. 验证数据集只由 MySQL 空卷入口执行，已有卷部署不重新应用；
3. 验证 `none` 清理 CRM 演示业务数据并保留首次登录所需基础链路；
4. 验证 `legacy-demo-v1` 保留上游演示数据；
5. 验证 cleanup 不进入 bootstrap 或 compatibility 清单；
6. 验证 `insert` 禁止 cleanup，`replace` 要求 cleanup 为 manifest 第一项；
7. 验证配置检查不改变当前 Pod 状态。
