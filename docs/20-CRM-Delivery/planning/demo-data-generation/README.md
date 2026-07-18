# 可重复 CRM 广覆盖演示数据生成与替换

制定日期：2026-07-18。实施分支：`develop`。

## 目标与边界

生成关系可靠、适合 CRM 统计、财务、营销、OA 和工单联合验收的演示数据。生成器只输出 SQL、manifest
和 checksum，不连接数据库，也不调用部署脚本；数据替换仅由 `deploy.sh` 消费已生成 manifest。

当前生成入口仍读取显式 YAML；KDL 已列为下一阶段唯一配置目标，迁移计划见
`../kdl-config-migration/README.md`。迁移期间不改变生成与部署分离原则。

## 当前规模

| 对象 | 数量 |
|---|---:|
| 客户 / 联系人 / 线索 / 跟进 | 120 / 120 / 120 / 300 |
| 商机 / 阶段 / 产品 / 合同 | 180 / 5 / 80 / 72 |
| 回款计划 / 回款 / 发票 / 报销 / 退款 | 144 / 90 / 60 / 60 / 30 |
| 营销活动 / 客户关怀 | 36 / 120 |
| OA 日程 / OA 任务 / CRM 工单 | 100 / 150 / 160 |

时间范围为 2025-07-01 至 2026-07-18。固定 seed 和相同配置会生成相同业务键、金额和状态分布；
数据库自增 ID 不作为确定性输出的一部分。

## 统计覆盖设计

- 合同少于商机，保留开放、赢单、输单和无效商机；
- 批次拥有独立五阶段流程，每个阶段必须至少有一条开放商机；
- 开放商机有预计成交时间，赢单有实际结单时间；
- 客户覆盖地区、行业、来源、等级和生命周期维度；
- 回款计划同时覆盖已回款、未回款逾期和未回款未来计划；
- 发票、回款、报销、退款、营销关怀和工单均覆盖多个合法状态；
- 审批完成记录只标记为导入历史，不伪造 Flowable 实例；可编辑草稿保留真实前端操作能力。

## 使用

```bash
bash podman/operations/database/generate-demo-dataset.sh \
  podman/config/generate-demo-dataset.example.yaml
```

将 `operation.mode` 设为 `check` 时只校验配置；设为 `generate` 时写入被 Git 忽略的
`database/generated/crm-demo-v2/`。运行库替换时，本机配置临时使用：

```yaml
mysql:
  dataset: crm-demo-v2
  dataset_manifest: ../../database/generated/crm-demo-v2/crm-demo-v2.manifest
  dataset_mode: replace
```

执行 `bash podman/deploy.sh <runtime-config>` 后必须把 `dataset_mode` 恢复为 `preserve`。

## 可执行质量门禁

manifest 最后一项为 `04-validate.sql`，会在服务启动前校验数量、阶段非空、四类结单结果、预测与实际、
客户画像、逾期与未来计划、多状态覆盖和关键引用。任一断言失败都会中止部署，不允许启动一个看似有数据、
实际统计退化的环境。
