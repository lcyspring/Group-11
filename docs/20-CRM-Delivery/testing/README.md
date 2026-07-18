# CRM 测试目录

每个功能或核心缺陷使用独立目录，固定包含测试计划、执行结果和覆盖率记录。

## 当前基线

更新日期：2026-07-18

- Ubuntu 26.04 容器 CRM 自动化：527/527，失败 0、错误 0、跳过 0。
- CRM JaCoCo：指令 49.55%、分支 42.53%、行 48.26%、方法 37.56%。
- Web 全仓 `vue-tsc --noEmit`：脚本与模板诊断 0；标准 Ubuntu YAML 入口通过。
- 运行安全框架专项：Security 3/3、CORS 2/2；真实 HTTP 安全矩阵全部通过。
- ERP 履约专项：4/4；ERP 模块 JaCoCo 行覆盖率 6.92%。
- Ubuntu 26.04 Infra 文件专项：28/28；模块行覆盖率 8.16%。
- Ubuntu 26.04 BPM 自动化：54 个，48 通过、6 跳过、失败 0；行覆盖率 8.58%。
- Ubuntu 26.04 容器 CRM 前端：统计纯函数与 SFC 契约 11/11，行 95.49%、分支 95.83%、函数 100%；发票 3/3。
- 原始报告：`Server/mitedtsm-module-crm/target/site/jacoco/`，属于构建产物，不提交。

## 目录索引

| 范围 | 类型 |
|---|---|
| `database-lifecycle-governance/` | MySQL 基线、迁移、种子、修复、清理、销毁分层及 manifest 门禁 |
| `crm-user-guide-routes/` | 用户指南路由、运行菜单和前端组件一致性 |
| `podman-operations-documentation/` | 编译、构建、部署操作与三类 YAML 字段参考 |
| `crm-database-backup-recovery/` | CRM MySQL 一致性备份、校验、隔离恢复和真源保护 |
| `build-toolchain-image-portability/` | Ubuntu 26.04 与 HBuilderX 编译镜像 OCI save/load/push 边界 |
| `customer-duplicate-check/` | 客户名称查重功能与缺陷 |
| `contact-primary/` | 首联系人生命周期 |
| `contact-mobile-unique/` | 联系人手机号唯一性 |
| `customer-contact-filter/` | 客户联系人筛选 |
| `clue-primary-contact/` | 线索转换创建首联系人 |
| `clue-readonly/` | 已转换线索只读 |
| `customer-hierarchy/` | 客户上下级关系 |
| `customer-owner-history/` | 客户归属历史 |
| `customer-pool-concurrency/` | 公海并发领取 |
| `customer-name-validation/` | 客户名称长度边界 |
| `receivable-submit-amount-guard/` | 回款提交金额保护 |
| `follow-up-statistics-correctness/` | 跟进字段映射与逻辑删除统计 |
| `follow-up-statistics-interval-dedup/` | 跟进客户跨周期去重 |
| `sales-funnel-metric-contract/` | 漏斗转化指标契约 |
| `customer-deal-status-statistics/` | 客户成交状态分布 |
| `business-loss-closure/` | 商机输单/无效原因与并发保护 |
| `sales-forecast/` | 活跃商机销售预测汇总与明细 |
| `customer-financial-metric-contract/` | 客户成交与回款指标契约 |
| `customer-deal-top10/` | 客户成交金额 TOP10 排名与钻取 |
| `customer-region-distribution/` | 客户城市、省份、国家分布与层级聚合 |
| `business-stage-funnel/` | 商机状态组、累计阶段漏斗及相邻转化率 |
| `business-stage-funnel-drilldown/` | 阶段漏斗授权分页钻取及统计明细一致性 |
| `performance-target-foundation/` | 三层五类业绩目标数据源、维护约束和 MySQL 迁移 |
| `performance-target-completion/` | 五类目标、实际值、完成率统一契约及前端展示 |
| `performance-target-management/` | 三层五类目标维护、输入校验及前端精确汇总 |
| `business-stage-forward-guard/` | 商机阶段前向约束、推进说明和操作日志审计 |
| `business-to-contract-conversion/` | 赢单商机授权、继承、幂等转换合同和 MySQL 唯一键 |
| `contract-approval-revision-resubmit/` | 合同审批状态映射、修订重提、对象权限及回调幂等 |
| `contract-sign-contact-integrity/` | 合同签约联系人必须属于合同客户的服务端不变量 |
| `receivable-approval-revision-resubmit/` | 回款修订重提、金额边界、权限、回调幂等及生效汇总 |
| `receivable-plan-effective-status/` | 计划金额守恒、审批生效状态、逾期待办及展示一致性 |
| `crm-work-order-minimum-closure/` | 客服工单状态机、权限、轨迹、通知、待办和运行闭环 |
| `crm-work-order-statistics/` | 工单汇总、状态、类型、处理人和创建/完结趋势统计 |
| `customer-360-work-orders/` | 客户 360 工单分页、详情入口和对象范围复用 |
| `customer-360-read-model/` | 客户 360 聚合口径、权限范围、真实 SQL、专项构建和覆盖率 |
| `crm-work-order-dispatch/` | 待处理工单分派、并发保护、轨迹通知和多维视图筛选 |
| `crm-invoice-lifecycle/` | 发票草稿、开具、红冲、作废、金额守恒、Provider 幂等和前端状态机 |
| `crm-contract-lifecycle/` | 合同附件、实际签署、Provider 能力、作废、幂等和不可变轨迹 |
| `crm-contract-amendment/` | 已签署合同补充协议快照、独立审批、版本生效、财务下限、附件锁定和真实 API 验收 |
| `customer-four-state-lifecycle/` | 客户四态命令、成交兼容、不可变历史、筛选与画像统计 |
| `bpm-tenant-unified-backlog/` | BPM 租户查询、模型旁路、统一待办、构建和覆盖率 |
| `statistics-lineage-refresh/` | 六类统计指标血缘、域权限、实时刷新和覆盖率 |
| `crm-statistics-runtime-security/` | 临时 SELF 用户真实数据对账、跨人员/跨域/跨租户负向和清理 |
| `crm-statistics-ui-regressions/` | 乱码 ref、分页、年份模型和 loading 异常路径的 SFC 契约回归 |
| `deal-cycle-data-quality/` | 负成交周期保留、负样本显式标记、四类 API/MySQL 对账和覆盖率 |
| `receivable-reference-integrity/` | 回款历史引用四级状态、原始 ID、查询保护和统计双口径对账 |
| `performance-target-runtime-migration/` | 业绩目标迁移清单、运行表/权限/三语数据、API 和失败态 |
| `crm-receivable-refund/` | 回款退款/冲销金额守恒、审批状态机、对象权限、动作轨迹和运行验收 |
| `crm-resource-security/` | 八类导出对象权限、合同受保护附件、公共文件隔离和历史通知乱码 |
| `customer-360-refund/` | 退款/冲销审批金额、净回款、待回款和客户明细入口 |
| `customer-public-pool-policy/` | 客户公海状态、保护、回收、领取额度/冷却、迁移和运行验收 |
| `customer-garbage-lifecycle/` | 客户垃圾池管理员隔离、迁移、恢复、永久删除和引用保护 |
| `customer-garbage-feedback-refresh/` | 转入垃圾池专属反馈、拒绝零写入和缓存页激活刷新 |
| `clue-public-pool-lifecycle/` | 公共线索状态、迁移、领取/分配、额度、冷却、权限和容量并发保护 |
| `clue-activity-migration/` | 任务、通话、短信状态轨迹、权限和线索转客户同事务迁移 |
| `crm-business-quote-versioning/` | 报价锁定/重开、币种税率、产品版本、合同快照和真实 API 验收 |
| `crm-contract-erp-fulfillment/` | 合同履约资格、主数据映射、双侧幂等、多币种冻结、ERP 不可变保护和状态回传 |
| `crm-work-order-collaboration/` | 处理组、自动派单、未分配领取、抄送协作、组级/跨组权限和真实 API 验收 |
| `crm-work-order-service-governance/` | 移动签到、地理围栏、SLA 工作日历、暂停恢复和自动升级 |
| `crm-marketing/` | 营销活动、竞争对手、客户关怀、同意/退订、审核和短信/邮件群发 |
| `admin-legacy-media-orb/` | 管理端退休媒体源、历史头像、响应/缓存归一化和 ORB 防护 |
| `table-action-and-approval-ui/` | 全量操作列收敛、财务弹窗本地化及 BPM 缓存待办自动刷新 |
| `crm-contract-detail-initialization/` | 合同详情延迟挂载、路由编号校验及生命周期空编号防御 |
| `crm-dialog-i18n/` | CRM 全树弹窗确认/取消多语言键治理 |
| `receivable-contract-candidates/` | 可回款合同权限候选、剩余额度、合同优先选择及运行对账 |
| `crm-marketing-campaign-management/` | 营销活动真实负责人、完整字段、五态操作和草稿受控删除 |
| `bpm-orphan-approval-recovery/` | 孤立审批审计恢复、详情顺序加载和 Promise 异常收敛 |
| `crm-marketing-outreach/` | 合规群发名单、审核、调度、发送、重试和收件人结果完整闭环 |
| `crm-marketing-delivery-analytics/` | 短信送达、邮件接受/首次打开、匿名像素、对象范围和分口径比率 |
| `mall-h5-runtime-dependencies/` | Mall 依赖容器运行时安装、named volume 隔离和断网 HBuilderX 编译 |
| `crm-customer-care-management/` | 客户关怀计划、生日/节假日/成交后回访、自动触达、记录和生日查询 |
| `crm-runtime-security-baseline/` | Podman 显式安全配置、管理端点、Mock Token、CORS 和凭据外置 |
| `receivable-form-localization/` | 回款及回款计划负责人标签和客户/合同下拉三语一致性 |
| `shared-shortcut-date-range/` | 快捷日期状态初始化和 Promise 异常收敛 |
| `mp-account-empty-state/` | 公众号账号空状态导航、无通知行为和初始化异常收敛 |
| `oa-work-report/` | OA 日/周/月工作报告周期、接收人可见性、提交锁定和真实 API 验收 |
| `crm-customer-import-preview/` | 客户导入预检、字段映射、确认幂等、Ubuntu 构建和覆盖率 |
| `crm-async-export-task/` | CRM 五态异步导出、三阶段权限复验、单次令牌、过期清理和覆盖率 |
| `crm-marketing-link-click/` | 营销逐收件人安全跳转、独立/累计点击、逐链接统计和覆盖率 |

运行入口统一为：

```bash
cd podman
bash ./compile.sh ./config/test-crm-ubuntu-26.04.yaml
```
