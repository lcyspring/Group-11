# 客户查重覆盖率

## 生成命令

```bash
cd Server
mvn -pl mitedtsm-module-crm -am \
  -DskipTests=false \
  -Dtest=CrmCustomerServiceImplTest,CrmCustomerControllerTest \
  -Dsurefire.failIfNoSpecifiedTests=false \
  org.jacoco:jacoco-maven-plugin:0.8.13:prepare-agent \
  test org.jacoco:jacoco-maven-plugin:0.8.13:report
```

原始报告：`Server/mitedtsm-module-crm/target/site/jacoco/`（构建产物，不提交）。

## JaCoCo 结果

| 范围 | 指令 | 分支 | 行 | 方法 |
|---|---:|---:|---:|---:|
| CRM 模块整体基线 | 6.02% | 2.15% | 4.66% | 2.40% |
| `CrmCustomerServiceImpl` 类 | 16.39% | 8.82% | 16.54% | 21.28% |
| 新增 Service 查重方法 | 100% | 100% | 100% | 100% |
| 新增 Controller 查重方法 | 100% | 无分支 | 100% | 100% |
| 查重请求条件判断 | 100% 指令 | 87.50% | 100% | 100% |

说明：模块和既有大类覆盖率较低，是现有 112 个类的大量代码没有自动测试造成，不能用新增方法的 100% 掩盖。Mapper 默认方法已由真实 Podman API 执行，但此次 JaCoCo 只采集 JVM 单元测试，因此 Mapper 集成路径不计入代码覆盖率。

前端当前没有配置 Vitest/Istanbul 代码覆盖率；本次只记录 ESLint、生产构建和 Podman 资产/交互场景证据，不能声称前端代码覆盖率。后续应在统一测试基建中补组件测试与覆盖率采集。
