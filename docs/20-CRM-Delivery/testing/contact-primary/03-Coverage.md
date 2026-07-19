# 首联系人覆盖率

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
| CRM 模块整体 | 12.36% (1916/15498) | 10.00% (79/790) | 11.06% (340/3075) | 6.57% (61/929) |
| `CrmContactServiceImpl` 类 | 58.20% (394/677) | 58.11% (43/74) | 61.73% (100/162) | 50.00% (15/30) |
| 6 个首联系人核心辅助方法合计 | 90.91% (150/165) | 76.47% (26/34) | 88.89% (40/45) | 100% (6/6) |
| `unsetPrimaryContact` 冲突保护 | 100% | 100% | 100% | 100% |
| 批量首联系人 Service 方法 | 100% | 100% | 100% | 100% |

测试运行结果为 33/33，其中联系人专项 16/16。锁后当前读的四个更新/删除分支均由单元测试覆盖；真实 MySQL `REPEATABLE READ` 锁等待时序另由 Podman 集成测试 2/2 验证。

说明：Mapper 注解 SQL 和真实并发路径由 Podman 集成场景执行，但 JaCoCo 只采集单元测试 JVM，因此这些真实 SQL 路径不计入报告。前端尚未配置组件覆盖率；本次只能记录 ESLint、生产构建和真实 API/资产验证，不能声称前端代码覆盖率。
