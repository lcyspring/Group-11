# 数据库显式运行配置测试结果

执行日期：2026-07-18。

| 检查 | 结果 |
|---|---|
| KDL 数据库连接字段完整读取 | 通过，157 份正常 KDL 可解析 |
| Server master JDBC URL 显式注入 | 通过 |
| local profile 固定 master URL 扫描 | 通过，残留 0 |
| `mysql.sql_root` 与 manifest 边界 | 通过 |
| 隔离 MySQL 空库/已有库/未知库/require-existing | 通过，4/4 |
| 营销 Provider 管理凭据回归 | 通过，4/4 |
| deploy check 保持 Pod/数据不变 | 通过，Pod 保持 Running |
