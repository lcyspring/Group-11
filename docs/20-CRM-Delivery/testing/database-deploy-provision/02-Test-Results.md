# 测试结果：部署期数据库 provision

执行日期：2026-07-18。

命令：

```bash
bash podman/tests/database-deploy-provision/run.sh podman/config/runtime-local.kdl
```

| 用例 | 结果 |
|---|---|
| 空库 bootstrap + compatibility + `none` 数据集 | 通过，343 张表、`system_users=1`、`crm_customer=0` |
| 已有完整库重复执行 | 通过，数据保留且管理员未重复 |
| 非空未知库保护 | 通过，脚本按预期拒绝 |
| `require-existing` 空库保护 | 通过，脚本按预期拒绝 |

汇总：4/4 通过，失败 0。测试结束后临时容器与卷已清理。

另执行真实 `deploy.sh replace`：现有数据库识别为 388 张表，bootstrap 和数据集均跳过，兼容清单
幂等完成；Server、Web、Mall 健康。
