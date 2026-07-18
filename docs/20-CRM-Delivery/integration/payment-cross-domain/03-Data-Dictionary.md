# 数据字典与状态映射

## 稳定映射键

| 字段 | 来源 | 约束 |
|---|---|---|
| `tenant_id` | 由 Pay 应用/业务对象服务端推导 | 不接受客户端自由指定 |
| `business_type` | `CRM_RECEIVABLE`、`CRM_RECEIVABLE_REFUND`、未来显式枚举 | 禁止自由文本域名 |
| `business_id` | 业务聚合主键 | 与类型、租户联合唯一 |
| `pay_app_id` | Pay 应用 | 必须属于同一租户并匹配 KDL 逻辑引用 |
| `merchant_order_id` | 业务域稳定生成 | 在 Pay App 内唯一；重试不变 |
| `pay_order_id` | Pay 创建结果 | 只可绑定一次，冲突失败关闭 |
| `merchant_refund_id` | CRM 客户退款稳定生成 | 在 Pay App 内唯一；业务冲销不得生成 |
| `pay_refund_id` | Pay 创建结果 | 只可绑定一次 |
| `channel_order_no/refund_no` | Pay/Provider | 仅作审计展示，不作跨域主键 |

建议新增独立 `crm_payment_settlement` 关联表，不把 Pay 字段散落进回款、退款、报销主表。建议唯一约束：

- `(tenant_id, business_type, business_id)`；
- `(tenant_id, pay_app_id, merchant_order_id)`；
- 有退款编号时 `(tenant_id, pay_app_id, merchant_refund_id)`；
- `(tenant_id, pay_order_id, pay_refund_id)` 的有效组合不得重复。

## 建议关联表字段

| 字段 | 类型/单位 | 说明 |
|---|---|---|
| `id` | bigint | 内部主键 |
| `tenant_id` | bigint | 租户 |
| `business_type/business_id` | varchar/bigint | 业务聚合引用 |
| `pay_app_id/pay_order_id/pay_refund_id` | bigint | Pay 引用 |
| `merchant_order_id/merchant_refund_id` | varchar(64) | 稳定幂等键 |
| `currency` | char(3) | ISO 4217 大写币种 |
| `amount_minor` | bigint | 最小货币单位整数；禁止浮点 |
| `settlement_status` | varchar(24) | `WAITING/SUCCEEDED/FAILED/CLOSED/UNKNOWN` |
| `provider_success_time` | datetime | Pay 查询得到的渠道成功时间 |
| `last_event_id` | varchar(64) | 最近已应用事件 |
| `version` | int | 乐观并发版本 |
| `creator/updater/create_time/update_time` | 审计字段 | 使用统一框架字段 |

当前 Pay 金额是整数“分”，CRM 回款/退款是 `BigDecimal`“元”，报销带三位币种。接入前必须建立币种
小数位目录并采用无损转换；例如 CNY 10.01 元映射 1001 分。超过精度、溢出、空币种均拒绝，不四舍五入。
现有 CRM 回款/退款没有币种字段，因此不能安全启用多币种在线结算。

## 状态映射

| Pay 状态 | 结算投影 | CRM 审批状态影响 |
|---|---|---|
| Order `0 WAITING` | `WAITING` | 无 |
| Order `10 SUCCESS` | `SUCCEEDED` | 仅记录结算证据，不自动改 `auditStatus` |
| Order `20 REFUND` | 原支付仍成功，另汇总退款 | 无；由退款聚合表达反向业务 |
| Order `30 CLOSED` | `CLOSED` | 无 |
| Refund `0 WAITING` | `WAITING` | 无 |
| Refund `10 SUCCESS` | `SUCCEEDED` | CRM 退款审批轨迹保持不变 |
| Refund `20 FAILURE` | `FAILED` | 进入重试/人工补偿，不回滚审批 |

CRM 审批状态 `0/10/20/30/40` 分别是未提交、审批中、通过、不通过、取消。只有 `20` 允许发起在线
结算；Pay 结果永远不能构造 BPM 审批通过事件。

## 审计最小字段

每次创建、通知、重试、冲突、人工补偿和对账修复都记录：租户、业务类型/ID、Pay App、商户编号、
Pay ID、事件 ID、旧/新结算状态、金额/币种、操作者或系统主体、追踪 ID、发生时间、失败分类和脱敏原因。
原始 Provider 报文只留在 Pay 受控事实中，CRM/OA 不复制；普通 INFO 日志不得记录报文、签名或用户标识。

