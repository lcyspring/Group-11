# 可重复演示数据生成与替换 Plan

制定日期：2026-07-18。实施分支：`develop`。

## 目标

生成比上游样例数量更大、关系更可靠、适合 CRM/OA/统计/审批联合验收的演示数据，同时保持生产
默认零替换。数据包可使用不同固定随机种子重复生成，结果可对账、可清理、可换版本。

## YAML 契约草案

```yaml
dataset_generation:
  mode: check
  dataset_name: crm-demo-v2
  random_seed: 20260718
  tenant_id: 1
  time_start: 2025-01-01
  time_end: 2026-12-31
  customer_count: 1000
  contacts_per_customer_max: 4
  business_count: 1600
  contract_count: 900
  receivable_plan_count: 1800
  work_order_count: 2000
  cleanup_existing_generated_data: false
  confirm_persistent_data_change: false
```

命令行只接收 YAML 路径。`check` 只生成规模估算与引用计划；`generate` 只输出版本化
SQL/manifest/checksum，且不连接数据库。生成与部署是独立链路；生成器绝不调用 `deploy.sh`。
`deploy.sh` 仍按运行 YAML 消费已经生成好的 manifest：默认 `existing_dataset_policy: preserve`，只有
显式改为 `replace` 并同时开启 cleanup 与持久数据确认才替换已有数据。部署脚本不会现场生成数据。

## 可靠性要求

- 所有记录带稳定批次标识，清理只命中本生成器数据；
- 客户、联系人、线索、商机、合同、产品快照、回款计划/回款/发票/报销/退款、工单和审批引用一致；
- 金额满足合同、计划、回款、退款、核销守恒；状态与动作轨迹一致；
- 时间分布覆盖未成交、成交、逾期、跨月、跨年和统计空桶；
- 固定 seed + 相同 YAML 生成相同业务键和分布；
- 生成后运行外键孤儿、金额守恒、状态轨迹、租户隔离、统计对账和数量断言；
- 真实个人信息、真实手机号、可投递邮箱和真实 Provider 授权一律禁止进入演示数据。

## 分阶段

1. 建立 YAML schema、规模计算和固定种子生成器；
2. 先覆盖 CRM 核心与财务真源，再覆盖工单、营销、OA 和 BPM 实例；
3. 输出 `crm-demo-v2` manifest、cleanup、insert 和校验报告；
4. 在临时数据库完成生成→校验→清理→换 seed→再生成；
5. 通过独立 `database-dataset.sh` 双确认替换本地演示数据，生产模板保持关闭。
