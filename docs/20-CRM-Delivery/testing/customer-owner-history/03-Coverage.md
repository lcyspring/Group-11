# CRM 客户归属记录覆盖率

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
| 指令 | 2551/16132 | 15.81% |
| 分支 | 117/838 | 13.96% |
| 行 | 466/3198 | 14.57% |
| 复杂度 | 117/1367 | 8.56% |
| 方法 | 83/948 | 8.76% |

重点类：

- `CrmCustomerServiceImpl`：指令 466/1469、分支 29/124、行 96/305、方法 18/52。
- `CrmCustomerController`：指令 129/668、分支 2/26、行 22/115、方法 9/41。

CRM 全量测试为 54/54。真实 MySQL 事务回滚、多租户拦截、CRM 数据权限 AOP、迁移幂等性和前端交互不进入单测 JVM，因此以 API/MySQL、ESLint 和生产构建作为补充证据，不计入 JaCoCo。
