# Podman 配置目录中文索引

## 日常配置

| 类型 | 模板/本机文件 | 用途 |
|---|---|---|
| Server/Web 构建 | `build-ubuntu-26.04.kdl` | Ubuntu 26.04 全量构建 |
| Mall H5 构建 | `build-mall-h5-ubuntu-26.04.kdl` | 无图形 HBuilderX 构建 |
| 四目标编译示例 | `compile-all-ubuntu-26.04.example.kdl` | `include_targets: all`、`exclude_targets: none`，一次选择全部产物 |
| 运行镜像封装 | `runtime-images.example.kdl` | 将已有产物封装为 Init/Server/Web/Mall 四个运行镜像，不启动容器 |
| Server 单项封装 | `runtime-images-server.example.kdl` | 只重新封装已编译 Server JAR |
| Web 单项封装 | `runtime-images-web.example.kdl` | 只重新封装已编译管理端产物 |
| 镜像封装预检 | `runtime-images-check.kdl` | 校验产物与封装配置，不改变镜像 |
| 运行预检 | `runtime-local-check.kdl` | 不改变 Pod、镜像和卷 |
| 本机运行 | `runtime-local.kdl` | ignored，保存真实本地凭据和 replace 模式 |
| 安全停服示例 | `cleanup-stop.example.kdl` | 删除 Pod，但保留 MySQL、Redis、RabbitMQ、TDengine 数据卷 |
| 数据重置示例 | `cleanup-reset.example.kdl` | 删除 Pod 和四个持久卷；仅用于明确要求的全新环境重建 |
| 单项热替换 | `runtime-local-replace-*.kdl` | ignored，仅消费预先封装镜像并替换 Server/Web/Mall |
| 数据保护 | `database-backup-check.kdl` | 备份/恢复安全模板 |
| 编译镜像 | `build-image-archives-check.kdl` | 工具链镜像 check/save/load/push 模板 |
| CRM 性能基线 | `verify-crm-performance-baseline.example.kdl` | 只读并发负载与阈值共享模板 |
| 工单 50 并发专项 | `verify-crm-work-order-performance.example.kdl` | 创建、SLA、流转、统计、完结与退出清理 |
| 工单安全负向专项 | `verify-crm-work-order-security.example.kdl` | 临时受限角色、越权、跨租户、状态、注入/XSS 和边界 |
| 演示数据生成 | `generate-demo-dataset.example.kdl` | 独立生成 SQL/manifest/checksum；不连接数据库，不由部署调用 |
| CRM 诊断包 | `crm-diagnostics.example.kdl` | SLI 阈值、日志窗口和本机诊断输出模板 |
| 营销点击验收 | `verify-crm-marketing-link-click.example.kdl` | 白名单跳转、令牌、原子累计和独立点击统计 |

共享运行模板默认使用 `network.host_address: 0.0.0.0`，因此 Server、Web 和 Mall 会监听全部主机网络接口；
`security.cors_allowed_origins: "*"` 允许任意浏览器来源携带显式 `Authorization`、`tenant-id` 等请求头，
并要求 `security.cors_allow_credentials: false`。这里的 credentials 指 Cookie/HTTP 凭据模式，不影响
Bearer Token。部署主机必须用 UFW、云安全组或上游反向代理限制可信来源，不能把数据库和消息中间件端口暴露出去。

## BPM 流程恢复示例

所有 `.example.kdl` 都是必须提交、可审查的配置契约，不能加入 `.gitignore`。示例只能使用
`CHANGE_ME` 等占位值，不能写真实密码。先复制为 ignored 的 `*-local.kdl`，再填写本机账号：

```bash
cp ./config/bpm-provision-all.example.kdl ./config/bpm-provision-all-local.kdl
cp ./config/bpm-provision-receivable.example.kdl ./config/bpm-provision-receivable-local.kdl
cp ./config/bpm-provision.example.kdl ./config/bpm-provision-local.kdl
cp ./config/bpm-provision-contract.example.kdl ./config/bpm-provision-contract-local.kdl
cp ./config/bpm-provision-refund.example.kdl ./config/bpm-provision-refund-local.kdl
cp ./config/bpm-provision-trip.example.kdl ./config/bpm-provision-trip-local.kdl
cp ./config/bpm-provision-loan.example.kdl ./config/bpm-provision-loan-local.kdl
cp ./config/bpm-provision-leave.example.kdl ./config/bpm-provision-leave-local.kdl
cp ./config/bpm-provision-customer-visit.example.kdl ./config/bpm-provision-customer-visit-local.kdl
```

| 示例 | 受管流程 |
|---|---|
| `bpm-provision-all.example.kdl` | 聚合清单；一次恢复请假、回款、报销、合同、退款、出差、借款、客户拜访和请示 |
| `bpm-provision-receivable.example.kdl` | CRM 回款审批 `crm-receivable-audit` |
| `bpm-provision.example.kdl` | CRM 报销审批 `crm-reimbursement-audit` |
| `bpm-provision-contract.example.kdl` | CRM 合同审批 `crm-contract-audit` |
| `bpm-provision-amendment.example.kdl` | CRM 合同补充协议审批 `crm-contract-amendment-audit` |
| `bpm-provision-refund.example.kdl` | CRM 退款/冲销审批 `crm-receivable-refund-audit` |
| `bpm-provision-trip.example.kdl` | OA 出差审批 `oa_trip` |
| `bpm-provision-loan.example.kdl` | OA 借款审批 `oa_loan` |
| `bpm-provision-leave.example.kdl` | OA 请假审批 `oa_leave` |
| `bpm-provision-customer-visit.example.kdl` | CRM 客户拜访审批 `crm_customer_visit_audit` |

聚合清单中的相对路径以清单所在目录解析。正式部署应让 ignored 的
`bpm-provision-all-local.kdl` 引用各 ignored 单模型配置；全新数据卷启动时，运行配置设置
`bpm.provision_after_start: true`，`deploy.sh replace` 会在 Server 健康后恢复全部流程定义。

若现有环境只缺少请假模型，可在填写 ignored 本机配置后单独幂等补配：

```bash
bash ./operations/bpm/provision-bpm-model.sh ./config/bpm-provision-leave-local.kdl
```

命令成功必须输出已部署模型 `oa_leave`；重复执行只更新/复用同名角色、分类和模型，不创建第二套业务流程。

## 清理示例

清理入口仍然只接受一个 KDL 路径：

```bash
# 日常停服：保留全部持久数据
bash ./stop.sh ./config/cleanup-stop.example.kdl

# 灾难恢复演练或明确要求全新数据库时：永久删除四个数据卷
cp ./config/cleanup-reset.example.kdl ./config/runtime-reset-local.kdl
bash ./stop.sh ./config/runtime-reset-local.kdl
```

第二条命令只在 `remove_volumes_on_down` 与 `confirm_persistent_data_reset` 同时为 `true` 时执行，
并不可恢复地清除 MySQL、Redis、RabbitMQ、TDengine 数据。执行前应先使用
`operations/database/database-backup.sh` 完成备份，并确认下一次 `deploy.sh replace` 开启 BPM 自动恢复。构建产物不由
`stop.sh` 删除；Maven `target`、`Web/dist-prod`、Mall `unpackage/dist` 应通过对应构建配置的
clean 字段重建，避免把“清产物”和“销毁业务数据”混为一个操作。

## 测试配置

`verify-*`、`test-*`、`check-*` 和 `bpm-provision-*` 均是结构化测试/验收资产。它们记录测试模块、
账号、端点、期望值和 Ubuntu 构建开关，供 Bug/功能回归复现，不作为日常部署入口。
Web 任务必须同时显式填写 `web.coverage_enabled`：真正运行 Deno Test 的任务设为 `true` 并给出
`coverage_threshold`，纯 ESLint、`ts:check` 或空任务设为 `false`，避免把静态检查误报成测试覆盖率。
性能基线先复制共享模板为 ignored 的 `verify-crm-performance-baseline-local.kdl`，再填写真实账号；
报告写入 CRM 独立证据目录。
诊断模板复制为 ignored 的 `crm-diagnostics-local.kdl` 后填写真实 MySQL 账号；原始日志包只写入
ignored 的 `podman/diagnostics/`。

新增配置时应优先复用现有协议，不再为一个布尔值增加命令行参数。完整字段见
[KDL_FIELDS_ZH.md](KDL_FIELDS_ZH.md)。
