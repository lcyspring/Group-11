# CRM 公海并发领取覆盖率

执行日期：2026-07-14

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
| 指令 | 2603/16148 | 16.12% |
| 分支 | 122/838 | 14.56% |
| 行 | 477/3202 | 14.90% |
| 复杂度 | 120/1368 | 8.77% |
| 方法 | 86/949 | 9.06% |

重点类：

- `CrmCustomerServiceImpl`：指令 502/1469、分支 34/124、行 103/305、复杂度 30/114、方法 20/52。
- `CrmCustomerMapper`：指令 127/623、分支 8/24、行 31/128、复杂度 6/36、方法 3/24。

CRM 全量测试为 56/56。真实 MySQL 行锁调度、两个 HTTP 请求的竞争结果和有效权限唯一性不进入单测 JVM，
因此以 Podman API/MySQL 并发验证作为 JaCoCo 之外的必要证据。
