# CRM 统计真实数据与权限测试计划

环境：rootless Podman、Ubuntu 26.04 构建产物、MySQL 8.0。

1. 通过正式系统 API 创建临时 SELF 数据范围角色和用户；
2. 角色只授予 `crm:statistics-customer:query`；
3. 管理员创建一名由临时用户负责的客户；
4. 临时用户查询本人客户统计，API 结果与 MySQL 逐项对账；
5. 临时用户查询同部门其他用户和整部门，必须失败关闭；
6. 客户统计权限访问客户血缘目录成功，访问漏斗目录拒绝；
7. 相同令牌跨租户拒绝，无令牌拒绝；
8. 验收结束后清理客户、对象权限、用户、角色、菜单授权和令牌；
9. CRM 全量测试和 JaCoCo 覆盖率更新。

运行入口：

```bash
bash podman/tests/acceptance/verify-crm-statistics-runtime.sh \
  podman/config/verify-crm-statistics-runtime-local.yaml
```

命令行只接受 YAML 路径；含凭据的 `*-local.yaml` 已由 Git 忽略。
