# CRM 客户导入预检测试结果

日期：2026-07-17。环境：Ubuntu 26.04 构建容器。

| 检查 | 结果 |
|---|---|
| CRM 全量自动化 | 509/509，通过；失败 0、错误 0、跳过 0 |
| 导入预检 Service 专项 | 5/5，通过 |
| Web 契约测试 | 3/3，通过 |
| Web 专项 ESLint | 零警告 |
| Web production build | 通过 |
| CRM JaCoCo 报告 | 已生成 |
| MySQL 增量迁移 | 当前数据库卷执行成功 |
| MySQL 增量迁移重入 | Server 重建时重复执行成功 |
| 真实 HTTP 预检 | 2 行可创建；预检后客户表新增 0 条 |
| 真实 HTTP 确认幂等 | 首次 2 条、再次仍为 2 条且结果一致 |
| 验收数据清理 | 客户、权限、归属历史和预检快照残留 0 条 |

专项入口：

```bash
bash podman/compile.sh podman/config/verify-crm-customer-import-ubuntu-26.04.yaml
bash podman/tests/acceptance/verify-crm-customer-import.sh podman/config/verify-crm-customer-import.example.yaml
```

构建镜像为公开的 `ghcr.io/elel-code/group-11-build-ubuntu:26.04`，`rebuild: false`；容器运行时安装或
复用 named-volume 中的项目依赖，Host 不安装 `node_modules`。
