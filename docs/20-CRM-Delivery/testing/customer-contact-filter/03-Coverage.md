# 客户联系人筛选覆盖率

## 生成命令

```bash
cd Server
mvn -pl mitedtsm-module-crm -am \
  '-Dtest=Crm*Test' \
  -Dsurefire.failIfNoSpecifiedTests=false \
  org.jacoco:jacoco-maven-plugin:0.8.13:prepare-agent \
  test org.jacoco:jacoco-maven-plugin:0.8.13:report
```

原始报告：`Server/mitedtsm-module-crm/target/site/jacoco/`（构建产物，不提交）。

## JaCoCo 结果

| 范围 | 指令 | 分支 | 行 | 方法 |
|---|---:|---:|---:|---:|
| CRM 模块整体 | 13.01% (2023/15553) | 10.93% (87/796) | 11.85% (366/3089) | 6.77% (63/930) |
| `CrmCustomerMapper` | 18.90% (107/566) | 33.33% (8/24) | 22.41% (26/116) | 9.52% (2/21) |

测试运行结果为 36/36，其中本功能查询结构测试 3/3。真实数据库生成 SQL、租户/逻辑删除注入和分页结果由 Podman API 测试 5/5 验证，不属于单元测试 JVM 覆盖率。
