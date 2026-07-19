# 联系人手机号唯一性覆盖率

## 生成命令

```bash
cd Server
mvn -pl mitedtsm-module-crm -am \
  '-Dtest=Crm*Test' \
  -Dsurefire.failIfNoSpecifiedTests=false \
  org.jacoco:jacoco-maven-plugin:0.8.13:prepare-agent \
  test org.jacoco:jacoco-maven-plugin:0.8.13:report
```

原始报告：`Server/mitedtsm-module-crm/target/site/jacoco/`，属于构建产物，不提交。

## JaCoCo 结果

| 范围 | 指令 | 分支 | 行 | 方法 |
|---|---:|---:|---:|---:|
| CRM 模块整体 | 12.15% (1878/15457) | 9.44% (74/784) | 10.77% (330/3064) | 6.47% (60/928) |
| `CrmContactServiceImpl` | 56.45% (363/643) | 55.88% (38/68) | 59.87% (91/152) | 48.28% (14/29) |
| `normalizeMobile` | 100% | 无分支 | 100% | 100% |
| `validateMobileUnique` | 100% | 100% | 100% | 100% |

CRM 测试运行结果为 29/29，其中联系人专项 12/12。真实 MySQL 锁、生成列和唯一索引路径由 Podman 测试覆盖，不进入单元测试 JVM 的 JaCoCo 数据。前端尚无组件覆盖率，当前只记录 ESLint、生产构建和真实 API 验证。
