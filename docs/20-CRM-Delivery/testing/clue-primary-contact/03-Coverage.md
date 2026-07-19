# 线索转客户创建首联系人覆盖率

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
| CRM 模块整体 | 13.24% (2065/15595) | 10.93% (87/796) | 12.02% (372/3095) | 6.77% (63/930) |
| `CrmClueServiceImpl` | 32.55% (111/341) | 41.67% (5/12) | 30.88% (21/68) | 23.08% (3/13) |

测试运行结果为 39/39。新增测试覆盖请求字段约束、抢占失败短路、成功路径调用顺序和首联系人字段复制。

真实数据库并发和故障注入不在单元测试 JVM 内，因此不会进入 JaCoCo：同一线索双请求竞争、联系人 INSERT 异常触发事务回滚、数据库权限与回显均以 Podman API/MySQL 断言作为补充证据。前端尚未配置组件覆盖率，本阶段只记录 ESLint、生产构建和已部署资产验证，不声称前端代码覆盖率。
