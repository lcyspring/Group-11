# Podman 配置目录中文索引

## 日常配置

| 类型 | 模板/本机文件 | 用途 |
|---|---|---|
| Server/Web 构建 | `build-ubuntu-26.04.yaml` | Ubuntu 26.04 全量构建 |
| Mall H5 构建 | `build-mall-h5-ubuntu-26.04.yaml` | 无图形 HBuilderX 构建 |
| 运行预检 | `runtime-local-check.yaml` | 不改变 Pod、镜像和卷 |
| 本机运行 | `runtime-local.yaml` | ignored，保存真实本地凭据和 full 模式 |
| 安全停服示例 | `cleanup-stop.example.yaml` | 删除 Pod，但保留 MySQL、Redis、RabbitMQ、TDengine 数据卷 |
| 数据重置示例 | `cleanup-reset.example.yaml` | 删除 Pod 和四个持久卷；仅用于明确要求的全新环境重建 |
| 单项热替换 | `runtime-local-rebuild-*.yaml` | ignored，仅替换 Server/Web/Mall |
| 数据保护 | `database-backup-check.yaml` | 备份/恢复安全模板 |
| 编译镜像 | `build-image-archives-check.yaml` | 工具链镜像 check/save/load/push 模板 |
| CRM 性能基线 | `verify-crm-performance-baseline.example.yaml` | 只读并发负载与阈值共享模板 |
| CRM 诊断包 | `crm-diagnostics.example.yaml` | SLI 阈值、日志窗口和本机诊断输出模板 |

## BPM 流程恢复示例

所有 `.example.yaml` 都是必须提交、可审查的配置契约，不能加入 `.gitignore`。示例只能使用
`CHANGE_ME` 等占位值，不能写真实密码。先复制为 ignored 的 `*-local.yaml`，再填写本机账号：

```bash
cp ./config/bpm-provision-all.example.yaml ./config/bpm-provision-all-local.yaml
cp ./config/bpm-provision-receivable.example.yaml ./config/bpm-provision-receivable-local.yaml
cp ./config/bpm-provision.example.yaml ./config/bpm-provision-local.yaml
cp ./config/bpm-provision-contract.example.yaml ./config/bpm-provision-contract-local.yaml
cp ./config/bpm-provision-refund.example.yaml ./config/bpm-provision-refund-local.yaml
cp ./config/bpm-provision-trip.example.yaml ./config/bpm-provision-trip-local.yaml
cp ./config/bpm-provision-loan.example.yaml ./config/bpm-provision-loan-local.yaml
cp ./config/bpm-provision-leave.example.yaml ./config/bpm-provision-leave-local.yaml
cp ./config/bpm-provision-customer-visit.example.yaml ./config/bpm-provision-customer-visit-local.yaml
```

| 示例 | 受管流程 |
|---|---|
| `bpm-provision-all.example.yaml` | 聚合清单；一次恢复请假、回款、报销、合同、退款、出差、借款和客户拜访 |
| `bpm-provision-receivable.example.yaml` | CRM 回款审批 `crm-receivable-audit` |
| `bpm-provision.example.yaml` | CRM 报销审批 `crm-reimbursement-audit` |
| `bpm-provision-contract.example.yaml` | CRM 合同审批 `crm-contract-audit` |
| `bpm-provision-amendment.example.yaml` | CRM 合同补充协议审批 `crm-contract-amendment-audit` |
| `bpm-provision-refund.example.yaml` | CRM 退款/冲销审批 `crm-receivable-refund-audit` |
| `bpm-provision-trip.example.yaml` | OA 出差审批 `oa_trip` |
| `bpm-provision-loan.example.yaml` | OA 借款审批 `oa_loan` |
| `bpm-provision-leave.example.yaml` | OA 请假审批 `oa_leave` |
| `bpm-provision-customer-visit.example.yaml` | CRM 客户拜访审批 `crm_customer_visit_audit` |

聚合清单中的相对路径以清单所在目录解析。正式部署应让 ignored 的
`bpm-provision-all-local.yaml` 引用各 ignored 单模型配置；全新数据卷启动时，运行配置设置
`bpm.provision_after_start: true`，`up.sh full` 会在 Server 健康后恢复全部流程定义。

若现有环境只缺少请假模型，可在填写 ignored 本机配置后单独幂等补配：

```bash
bash ./provision-bpm-model.sh ./config/bpm-provision-leave-local.yaml
```

命令成功必须输出已部署模型 `oa_leave`；重复执行只更新/复用同名角色、分类和模型，不创建第二套业务流程。

## 清理示例

清理入口仍然只接受一个 YAML 路径：

```bash
# 日常停服：保留全部持久数据
bash ./down.sh ./config/cleanup-stop.example.yaml

# 灾难恢复演练或明确要求全新数据库时：永久删除四个数据卷
cp ./config/cleanup-reset.example.yaml ./config/runtime-reset-local.yaml
bash ./down.sh ./config/runtime-reset-local.yaml
```

第二条命令不可恢复地清除 MySQL、Redis、RabbitMQ、TDengine 数据。执行前应先使用
`database-backup.sh` 完成备份，并确认下一次 `up.sh full` 开启 BPM 自动恢复。构建产物不由
`down.sh` 删除；Maven `target`、`Web/dist-prod`、Mall `unpackage/dist` 应通过对应构建配置的
clean 字段重建，避免把“清产物”和“销毁业务数据”混为一个操作。

## 测试配置

`verify-*`、`test-*`、`check-*` 和 `bpm-provision-*` 均是结构化测试/验收资产。它们记录测试模块、
账号、端点、期望值和 Ubuntu 构建开关，供 Bug/功能回归复现，不作为日常部署入口。
性能基线先复制共享模板为 ignored 的 `verify-crm-performance-baseline-local.yaml`，再填写真实账号；
报告写入 CRM 独立证据目录。
诊断模板复制为 ignored 的 `crm-diagnostics-local.yaml` 后填写真实 MySQL 账号；原始日志包只写入
ignored 的 `podman/diagnostics/`。

新增配置时应优先复用现有协议，不再为一个布尔值增加命令行参数。完整字段见
[YAML_FIELDS_ZH.md](YAML_FIELDS_ZH.md)。
