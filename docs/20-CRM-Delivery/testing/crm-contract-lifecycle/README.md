# CRM 合同生命周期测试记录

执行日期：2026-07-14。环境：rootless Podman、Ubuntu 26.04、OpenJDK 17.0.19、Maven 3.9.12、pnpm 11.3.0、MySQL 8.0。

## 结构化范围

| 层次 | 覆盖项 |
|---|---|
| Service | 审批状态、附件归属/类别、锁定删除、签署幂等、作废幂等、稳定 Provider 请求号 |
| Provider | `local-record` 只声明线下能力、不伪造外部签署编号 |
| 并发/事务 | 合同级行锁、签署/锁附件/写轨迹同事务、轨迹唯一序号 |
| API 安全 | 生命周期摘要不包含 `contractSnapshot`、`productSnapshot` 或 BaseDO 持久化字段 |
| 前端 | Provider 能力驱动方式选项、附件上传/删除、签署/作废、三语言文本 |
| MySQL | 三张表、唯一键、CHECK、三项权限及迁移重复执行 |
| 运行时 | 真实上传、签署、重复签署、锁定删除、作废、摘要查询和文件下载 |

后端专项代码：

- `service/contract/CrmContractLifecycleServiceImplTest`：7 条；
- `framework/contract/LocalRecordCrmContractSignProviderTest`：2 条；
- `controller/admin/contract/CrmContractLifecycleControllerTest`：1 条，锁定摘要字段边界；
- 合同既有 Service 测试同时验证生命周期依赖不破坏原审批和修改路径。

## 执行入口与结果

```bash
cd podman
bash ./build-in-ubuntu.sh ./config/test-crm-ubuntu-26.04.yaml
bash ./build-in-ubuntu.sh ./config/check-crm-contract-lifecycle-web-ubuntu-26.04.yaml
bash ./build-in-ubuntu.sh ./config/build-server-ubuntu-26.04.yaml
bash ./build-in-ubuntu.sh ./config/build-web-ubuntu-26.04.yaml
```

- CRM 全量：201/201，失败 0、错误 0、跳过 0；
- 合同生命周期 Service：7/7；本地 Provider：2/2；
- 合同生命周期前端 ESLint：0 警告；
- Server/Web Ubuntu 26.04 生产构建：通过。

## JaCoCo

| 指标 | 覆盖 | 已覆盖/总计 |
|---|---:|---:|
| 指令 | 39.31% | 9291/23634 |
| 分支 | 36.79% | 489/1329 |
| 行 | 37.00% | 1721/4651 |
| 方法 | 24.84% | 309/1244 |

原始 HTML/CSV 位于 `Server/mitedtsm-module-crm/target/site/jacoco/`，属于构建产物，不提交。

## 真实运行时验收

合同 13 上完成：上传签署副本、正式线下签署、相同命令重复返回原 ID、锁定附件删除返回
`1020000020`、签署作废。作废后摘要显示状态 20，动作类型为 7/8，序号为 1/2；响应扫描未出现
两个快照字段，签署副本下载返回 HTTP 200。

真实令牌、密码和本机含凭据 YAML 均未写入本文档。
