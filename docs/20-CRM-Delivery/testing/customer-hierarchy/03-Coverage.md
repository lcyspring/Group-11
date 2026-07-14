# 客户上下级关系覆盖率

执行日期：2026-07-13

## 生成命令

```bash
mvn -pl mitedtsm-module-crm -am \
  '-Dtest=Crm*Test' \
  -Dsurefire.failIfNoSpecifiedTests=false \
  org.jacoco:jacoco-maven-plugin:0.8.13:prepare-agent \
  test org.jacoco:jacoco-maven-plugin:0.8.13:report
```

## JaCoCo 结果

| 指标 | 已覆盖/总数 | 覆盖率 |
|---|---:|---:|
| 指令 | 2329/15926 | 14.62% |
| 分支 | 113/834 | 13.55% |
| 行 | 427/3162 | 13.50% |
| 复杂度 | 107/1356 | 7.89% |
| 方法 | 74/939 | 7.88% |

重点类：

- `CrmCustomerServiceImpl`：指令 364/1394、分支 27/122、行 77/292、方法 15/50。
- `CrmCustomerMapper`：指令 111/607、分支 8/24、行 27/124、方法 2/23。

CRM 测试运行结果为 50/50。真实 MySQL 租户拦截、行锁排队、递归无环断言、幂等迁移和前端路由交互不进入单元测试 JVM，因此以 API/MySQL、ESLint 和生产构建作为补充证据；不把这些场景虚报为 JaCoCo 覆盖。
