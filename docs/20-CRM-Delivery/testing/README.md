# CRM 测试目录

每个功能或核心缺陷使用独立目录，固定包含测试计划、执行结果和覆盖率记录。

## 当前基线

更新日期：2026-07-15

- Ubuntu 26.04 容器 CRM 自动化：354/354，失败 0、错误 0、跳过 0。
- CRM JaCoCo 基线：指令 47.23%、分支 44.00%、行 45.38%、方法 33.62%。
- Ubuntu 26.04 Infra 文件专项：28/28；模块行覆盖率 8.16%。
- Ubuntu 26.04 BPM 自动化：54 个，48 通过、6 跳过、失败 0；行覆盖率 8.58%。
- Ubuntu 26.04 容器 CRM 前端纯函数：统计 7/7，发票 3/3。
- 原始报告：`Server/mitedtsm-module-crm/target/site/jacoco/`，属于构建产物，不提交。

## 目录索引

| 范围 | 类型 |
|---|---|
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
| `customer-four-state-lifecycle/` | 客户四态命令、成交兼容、不可变历史、筛选与画像统计 |
| `bpm-tenant-unified-backlog/` | BPM 租户查询、模型旁路、统一待办、构建和覆盖率 |
| `statistics-lineage-refresh/` | 六类统计指标血缘、域权限、实时刷新和覆盖率 |
| `crm-receivable-refund/` | 回款退款/冲销金额守恒、审批状态机、对象权限、动作轨迹和运行验收 |
| `crm-resource-security/` | 八类导出对象权限、合同受保护附件、公共文件隔离和历史通知乱码 |
| `customer-360-refund/` | 退款/冲销审批金额、净回款、待回款和客户明细入口 |
| `customer-public-pool-policy/` | 客户公海状态、保护、回收、领取额度/冷却、迁移和运行验收 |
| `customer-garbage-lifecycle/` | 客户垃圾池管理员隔离、迁移、恢复、永久删除和引用保护 |
| `clue-public-pool-lifecycle/` | 公共线索状态、迁移、领取/分配、额度、冷却、权限和容量并发保护 |
| `clue-activity-migration/` | 任务、通话、短信状态轨迹、权限和线索转客户同事务迁移 |

运行入口统一为：

```bash
cd podman
bash ./build-in-ubuntu.sh ./config/test-crm-ubuntu-26.04.yaml
```
