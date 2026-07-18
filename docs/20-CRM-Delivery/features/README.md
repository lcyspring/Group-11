# CRM 功能实现日志

本目录只记录依据 Gap Analysis 和原型补齐的 CRM 功能，不与缺陷日志混放。

| ID | 功能 | 需求来源 | 状态 | 测试目录 |
|---|---|---|---|---|
| CRM-FEATURE-001 | 创建客户时按名称/手机查重并确认疑似重复 | US-CUS-002、GAP-CUS-001 | 已实现 | `docs/20-CRM-Delivery/testing/customer-duplicate-check/` |
| CRM-FEATURE-002 | 每客户唯一首联系人生命周期及客户列表回显 | US-CUS-010/011、GAP-CUS-002 | 已实现 | `docs/20-CRM-Delivery/testing/contact-primary/` |
| CRM-FEATURE-003 | 联系人手机号必填且同一客户内唯一 | CUS-009、GAP-CUS-002 | 已实现 | `docs/20-CRM-Delivery/testing/contact-mobile-unique/` |
| CRM-FEATURE-004 | 客户列表按任意联系人及首联系人筛选 | CUS-001、US-CUS-001、GAP-CUS-001 | 已实现 | `docs/20-CRM-Delivery/testing/customer-contact-filter/` |
| CRM-FEATURE-005 | 线索转客户时显式创建首联系人，并保证并发幂等和失败回滚 | US-LEAD-008、GAP-LEAD-002 | 已实现 | `docs/20-CRM-Delivery/testing/clue-primary-contact/` |
| CRM-FEATURE-006 | 已转换线索在字段、归属、跟进和团队权限层面强制只读 | GAP-LEAD-002、GAP-IAM-003 | 已实现 | `docs/20-CRM-Delivery/testing/clue-readonly/` |
| CRM-FEATURE-007 | 客户已成交/未成交状态分布 | US-RPT-009、GAP-RPT-001/002 | 阶段完成 | `docs/20-CRM-Delivery/testing/customer-deal-status-statistics/` |
| CRM-FEATURE-008 | 商机输单/无效原因及并发状态保护 | US-OPP-005、GAP-OPP-001 | 子闭环完成 | `docs/20-CRM-Delivery/testing/business-loss-closure/` |
| CRM-FEATURE-009 | 活跃商机销售预测汇总与明细 | US-RPT-001、OPP-009、GAP-RPT-001/002 | 已实现 | `docs/20-CRM-Delivery/testing/sales-forecast/` |
| CRM-FEATURE-010 | 客户成交、未回款和回款率服务端指标契约 | CUS-018、GAP-RPT-002 | 已实现 | `docs/20-CRM-Delivery/testing/customer-financial-metric-contract/` |
| CRM-FEATURE-011 | 客户成交金额 TOP10、稳定排名及客户钻取 | CUS-019、US-RPT-009、GAP-RPT-001/002 | 已实现 | `docs/20-CRM-Delivery/testing/customer-deal-top10/` |
| CRM-FEATURE-012 | 客户城市、省份、国家三层分布、正确聚合及授权客户钻取 | PG-0324/0326/0327、US-RPT-009 | 已实现 | `docs/20-CRM-Delivery/testing/customer-region-distribution/` |
| CRM-FEATURE-013 | 按商机状态组展示累计阶段数量、金额及相邻阶段转化率 | OPP-008、US-RPT-002、GAP-RPT-001/002 | 已实现 | `docs/20-CRM-Delivery/testing/business-stage-funnel/` |
| CRM-FEATURE-014 | 阶段漏斗授权分页钻取，统计与明细口径一致 | OPP-008、US-RPT-002、GAP-RPT-001/002 | 已实现 | `docs/20-CRM-Delivery/testing/business-stage-funnel-drilldown/` |
| CRM-FEATURE-015 | 个人、部门、公司五类月度业绩目标数据源、维护契约和约束 | US-MKT-001、US-RPT-005、GAP-MKT-001/RPT-001/002 | 阶段完成 | `docs/20-CRM-Delivery/testing/performance-target-foundation/` |
| CRM-FEATURE-016 | 五类月度/年度目标、实际值和完成率统一口径及前端展示 | US-RPT-005、GAP-RPT-001/002 | 已实现 | `docs/20-CRM-Delivery/testing/performance-target-completion/` |
| CRM-FEATURE-017 | 公司、部门、个人五类业绩目标前端维护及精确汇总 | US-MKT-001、US-RPT-005、GAP-MKT-001 | 已实现 | `docs/20-CRM-Delivery/testing/performance-target-management/` |
| CRM-FEATURE-018 | 商机阶段前向推进、必填推进说明及操作日志审计 | US-OPP-004、GAP-OPP-001、BC-03 | 子闭环完成 | `docs/20-CRM-Delivery/testing/business-stage-forward-guard/` |
| CRM-FEATURE-019 | 赢单商机显式、授权、幂等转换 CRM 合同草稿 | GAP-OPP-003、GAP-CTR-001/002、ADR-002～005 | 子闭环完成 | `docs/20-CRM-Delivery/testing/business-to-contract-conversion/` |
| CRM-FEATURE-020 | 合同审批状态映射、驳回/取消修订、重提和幂等回调 | GAP-CTR-002、GAP-ORD-003、GAP-APR-002、ADR-006 | 子闭环完成 | `docs/20-CRM-Delivery/testing/contract-approval-revision-resubmit/` |
| CRM-FEATURE-021 | 回款审批修订重提、权限/并发保护、正金额及生效汇总口径 | GAP-FIN-001/002、GAP-APR-002、ADR-006 | 子闭环完成 | `docs/20-CRM-Delivery/testing/receivable-approval-revision-resubmit/` |
| CRM-FEATURE-022 | 回款计划合同金额守恒、审批生效状态、逾期待办及前端统一口径 | GAP-FIN-001/002、GAP-RPT-002、ADR-007/008 | 子闭环完成 | `docs/20-CRM-Delivery/testing/receivable-plan-effective-status/` |
| CRM-FEATURE-023 | 客服工单来源、分派、状态机、轨迹、通知和前端操作最小闭环 | GAP-WO-001、ADR-010 | 子闭环完成 | `docs/20-CRM-Delivery/testing/crm-work-order-minimum-closure/` |
| CRM-FEATURE-024 | 客服工单接入个人待办并明确待办范围 | GAP-WO-001、GAP-APR-003 | 子闭环完成 | `docs/20-CRM-Delivery/testing/crm-work-order-minimum-closure/` |
| CRM-FEATURE-025 | 待处理工单手工分派、并发保护、轨迹通知和多维个人视图筛选 | US-WO-002/003、GAP-WO-001/002 | 子闭环完成 | `docs/20-CRM-Delivery/testing/crm-work-order-dispatch/` |
| CRM-FEATURE-026 | 合同产品名称、编码、单位和目录价不可变成交快照及历史回填 | GAP-OPP-002、GAP-PIM-001、ADR-003 | 子闭环完成 | `docs/20-CRM-Delivery/testing/contract-product-snapshot/` |
| CRM-FEATURE-027 | 合同发票草稿、正式开具、部分/全部红冲、蓝/红票作废、金额守恒、轨迹和外部适配边界 | GAP-FIN-001/002、REQ-PAY-003、ADR-009 | 子闭环完成 | `docs/20-CRM-Delivery/testing/crm-invoice-lifecycle/` |
| CRM-FEATURE-028 | 合同规范化附件、实际签署、适配器能力、作废和不可变版本轨迹 | GAP-CTR-001、ADR-002/004/015 | 子闭环完成 | `docs/20-CRM-Delivery/testing/crm-contract-lifecycle/` |
| CRM-FEATURE-029 | 客户潜在、意向、成交、流失四态命令、历史、筛选和画像统计 | GAP-CUS-003、GAP-RPT-001/002、ADR-018 | 已实现 | `docs/20-CRM-Delivery/testing/customer-four-state-lifecycle/` |
| CRM-FEATURE-030 | 客户 360 统一摘要、合同映射销售单据、财务净额、附件和发票关系视图 | US-CUS-003、GAP-CUS-003、ADR-002 | 已实现 | `docs/20-CRM-Delivery/testing/customer-360-read-model/` |
| CRM-FEATURE-031 | BPM 租户安全查询、模型治理及 CRM 流程审批统一待办 | GAP-APR-001/003、GAP-IAM-003 | 已实现 | `docs/20-CRM-Delivery/testing/bpm-tenant-unified-backlog/` |
| CRM-FEATURE-032 | 六类 CRM 统计指标 YAML 血缘目录、域权限隔离和显式实时刷新 | US-RPT-010、GAP-RPT-001～003 | 已实现 | `docs/20-CRM-Delivery/testing/statistics-lineage-refresh/` |
| CRM-FEATURE-033 | CRM 回款退款/业务冲销草稿、审批、金额守恒、不可变轨迹和对象权限闭环 | GAP-FIN-001/002、ADR-007/008 | 子闭环完成 | `docs/20-CRM-Delivery/testing/crm-receivable-refund/` |
| CRM-FEATURE-034 | 八类导出对象权限、合同受保护附件上传/下载及公共文件路由隔离 | GAP-IAM-003、GAP-SEC-003 | 已实现 | `docs/20-CRM-Delivery/testing/crm-resource-security/` |
| CRM-FEATURE-035 | 客户 360 退款/冲销记录、审批金额、净回款、待回款和权限化明细入口 | GAP-CUS-003、GAP-FIN-001/002、GAP-RPT-002 | 已实现 | `docs/20-CRM-Delivery/testing/customer-360-refund/` |
| CRM-FEATURE-036 | CRM 报销、费用明细、分类、受保护附件及审批修订闭环 | GAP-FIN-001/002、GAP-APR-002 | 子闭环完成 | `docs/20-CRM-Delivery/testing/crm-reimbursement/` |
| CRM-FEATURE-037 | 合同、回款、退款和报销四类审批模型的显式 YAML 治理、角色权限和幂等发布 | GAP-APR-001/002、GAP-IAM-003 | 已实现 | `docs/20-CRM-Delivery/testing/crm-core-approval-model-governance/` |
| CRM-FEATURE-038 | CRM 日期入参仅接受毫秒时间戳或其数字字符串，拒绝非法文本静默落入 1970 年 | GAP-FIN-002、GAP-RPT-002 | 已实现 | `docs/20-CRM-Delivery/testing/crm-datetime-input-contract/` |
| CRM-FEATURE-039 | 客户入池/领取时联系人负责人与 OWNER 权限原子同步，保留团队权限 | GAP-CUS-001/002、GAP-IAM-003 | 子闭环完成 | `docs/20-CRM-Delivery/testing/customer-pool-contact-ownership/` |
| CRM-FEATURE-040 | 客户公海显式状态、保护期、自动回收、领取额度/冷却、通知、配置和列表元数据完整闭环 | GAP-CUS-001、ADR-014 | 已实现 | `docs/20-CRM-Delivery/testing/customer-public-pool-policy/` |
| CRM-FEATURE-041 | 客户垃圾池管理员隔离、手工/自动迁移、恢复、引用保护永久删除及前端状态机 | GAP-CUS-001、ADR-014 | 已实现 | `docs/20-CRM-Delivery/testing/customer-garbage-lifecycle/` |
| CRM-FEATURE-042 | 公共线索显式状态、自动迁移、领取/分配额度、冷却、容量并发保护及前端状态机 | GAP-LEAD-001/002、ADR-014 | 已实现 | `docs/20-CRM-Delivery/testing/clue-public-pool-lifecycle/` |
| CRM-FEATURE-043 | 任务、通话、短信活动真源、状态轨迹及线索转客户同事务迁移审计 | GAP-LEAD-002、ADR-014 | 已实现 | `docs/20-CRM-Delivery/testing/clue-activity-migration/` |
| CRM-FEATURE-044 | 商机报价版本、锁定/重开、币种税率、SKU 版本与合同不可变价格快照 | GAP-OPP-002/003、GAP-PIM-001、ADR-003 | 已实现 | `docs/20-CRM-Delivery/testing/crm-business-quote-versioning/` |
| CRM-FEATURE-045 | 已签署 CRM 合同显式、双侧幂等创建 ERP 履约订单，主数据映射、多币种冻结和状态回传 | GAP-ORD-001/002/003、ADR-020 | 已实现 | `docs/20-CRM-Delivery/testing/crm-contract-erp-fulfillment/` |
| CRM-FEATURE-046 | 客服处理组、最小负载自动派单、未分配领取、抄送协作及组级/跨组权限闭环 | GAP-WO-001/002、ADR-021 | 已实现 | `docs/20-CRM-Delivery/testing/crm-work-order-collaboration/` |
| CRM-FEATURE-047 | 工单移动签到、地理围栏、SLA 工作日历、暂停和自动升级闭环 | GAP-WO-002、ADR-011 | 已实现 | `docs/20-CRM-Delivery/testing/crm-work-order-service-governance/` |
| CRM-FEATURE-048 | 营销活动、竞争对手、客户关怀及短信/邮件合规群发闭环 | GAP-MKT-001～003、ADR-016 | 已实现 | `docs/20-CRM-Delivery/testing/crm-marketing/` |
| CRM-FEATURE-049 | 已签署合同补充协议前后快照、独立审批、生效投影、金额下限和不可变依据附件闭环 | GAP-CTR-001/002、GAP-ORD-003、GAP-APR-002、ADR-015 | 已实现 | `docs/20-CRM-Delivery/testing/crm-contract-amendment/` |
| CRM-FEATURE-050 | 成交周期历史补录负样本保留、四类计数、显式数据质量警告和 YAML 口径 | GAP-RPT-001/002/003 | 已实现 | `docs/20-CRM-Delivery/testing/deal-cycle-data-quality/` |
| CRM-FEATURE-051 | 回款历史引用四级状态、原始 ID 保留、查询保护及客户统计/财务台账双口径治理 | GAP-FIN-001/002、GAP-RPT-002 | 已实现 | `docs/20-CRM-Delivery/testing/receivable-reference-integrity/` |
| CRM-FEATURE-052 | 营销活动真实负责人、完整字段维护、五态操作及草稿受控删除闭环 | GAP-MKT-001、ADR-016 | 已实现 | `docs/20-CRM-Delivery/testing/crm-marketing-campaign-management/` |
| CRM-FEATURE-053 | 合规群发名单、草稿维护、独立审核、计划群发、失败重试和收件人结果闭环 | GAP-MKT-002/003、ADR-016 | 已实现 | `docs/20-CRM-Delivery/testing/crm-marketing-outreach/` |
| CRM-FEATURE-054 | 客户关怀计划维护、生日/节假日/成交后回访、合规自动触达、记录和生日查询闭环 | GAP-MKT-002、ADR-016 | 已实现 | `docs/20-CRM-Delivery/testing/crm-customer-care-management/` |
| CRM-FEATURE-055 | Podman CRM 显式安全策略、凭据外置、管理端点收敛、可信 CORS 与弱密码哈希保护 | GAP-SEC-001/002、GAP-NFR-003/005 | 已实现 | `docs/20-CRM-Delivery/testing/crm-runtime-security-baseline/` |
| CRM-FEATURE-056 | 群发任务列表、详情、目标与收件人创建人/审核发送职责对象范围 | GAP-IAM-003、GAP-MKT-003、GAP-SEC-003 | 已实现 | `docs/20-CRM-Delivery/testing/crm-marketing-broadcast-object-scope/` |
| CRM-FEATURE-057 | 短信提供商送达回收、邮件服务商接受/首次打开、分口径比率及匿名像素 | GAP-MKT-003、ADR-016 | 已实现 | `docs/20-CRM-Delivery/testing/crm-marketing-delivery-analytics/` |
| CRM-FEATURE-058 | OA 日/周/月工作报告、周期唯一、接收人可见性、草稿维护和提交锁定闭环 | GAP-OA-003 | 已实现 | `docs/20-CRM-Delivery/testing/oa-work-report/` |
| CRM-FEATURE-059 | 客户 Excel 导入预检、自定义字段映射、逐行结论、过期隔离及幂等确认闭环 | 工作计划 CUS-09 | 已实现 | `docs/20-CRM-Delivery/testing/crm-customer-import-preview/` |
| CRM-FEATURE-060 | CRM 五态异步导出、权限快照、受保护结果文件和短期单次下载令牌 | 工作计划 CUS-10、GAP-IAM-003、GAP-SEC-003 | 已实现 | `docs/20-CRM-Delivery/testing/crm-async-export-task/` |
| CRM-FEATURE-061 | 营销群发逐收件人安全跳转、独立/累计点击及逐链接统计 | GAP-MKT-003、ADR-016 | 已实现 | `docs/20-CRM-Delivery/testing/crm-marketing-link-click/` |
| CRM-FEATURE-062 | 官方 MySQL 直接运行、部署期空库初始化、已有库兼容迁移和未知库保护 | 部署与数据治理 | 已实现 | `docs/20-CRM-Delivery/testing/database-deploy-provision/` |
| CRM-FEATURE-063 | 短信渠道、邮件账号及模板的显式 YAML 幂等 provision | GAP-MKT-003、ADR-016 | 已实现；真实供应商 UAT 待账号 | `docs/20-CRM-Delivery/testing/crm-marketing-provider-provision/` |
| CRM-FEATURE-064 | 固定 seed、规模可配、生成与部署分离的 CRM 核心/工单演示数据 | 数据与部署治理 | 第一版已实现；财务/BPM 待扩展 | `docs/20-CRM-Delivery/testing/deterministic-demo-data/` |
| CRM-FEATURE-065 | 商机阶段漏斗与赢单/输单/无效结果分布双视图、终态钻取及缺失初始阶段提示 | GAP-RPT-001/002 | 已实现 | `docs/20-CRM-Delivery/features/business-funnel-outcome-distribution/` |

说明：`CRM-FEATURE-005/043` 共同关闭线索转换的首联系人、跟进及任务/通话/短信活动迁移、并发幂等、审计和失败回滚。`CRM-FEATURE-007` 是历史二态基线，现已由 `CRM-FEATURE-029` 升级为四态模型。`CRM-FEATURE-015/016/017` 已建立目标事实源、完成度展示和三层目标维护入口；目标逐级分解合计规则仍待业务签署。`CRM-FEATURE-028` 负责合同签署事实与基础版本轨迹，正式补充协议独立审批由 `CRM-FEATURE-049` 完成；二者均不宣称接入外部电子签平台。`CRM-FEATURE-030/035` 已聚合退款真源，但不伪造尚未实现的 OA 任务真源。`CRM-FEATURE-033` 已完成真实 BPM 提交、驳回、修订、重提和通过验收。`CRM-FEATURE-034` 的新附件已受保护，历史公开附件仍需物理迁移。客户联系人归属、公海、垃圾池、公共线索、活动迁移、报价版本、ERP 履约和工单协作分别由 `CRM-FEATURE-039`～`046` 关闭。
