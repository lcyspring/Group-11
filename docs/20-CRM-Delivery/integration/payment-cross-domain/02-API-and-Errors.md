# API、事件与错误码

## 当前可复用接口

Pay 模块已有 Java API：

- `PayOrderApi.createOrder/getOrder/updatePayOrderPrice`；
- `PayRefundApi.createRefund/getRefund`；
- Provider 匿名入口 `/pay/notify/order/{channelId}`、`/refund/{channelId}` 和 `/transfer/{channelId}`；
- 支付成功业务通知体为 `merchantOrderId + payOrderId`；
- 退款结果业务通知体为 `merchantOrderId + merchantRefundId + payRefundId`。

当前 Pay 通知任务最多按既有频率执行 9 次并保留每次结果。只有 `CommonResult` 成功才确认消费。
这些能力不等于 CRM 已经实现接收端。

## 目标业务 API

以下是后续实现契约，当前不能作为已上线接口调用。

### 创建在线收款

`POST /admin-api/crm/receivable/payment-order/create`

```json
{
  "receivableId": 1001,
  "expectedVersion": 3,
  "expireTime": 1784304000000
}
```

响应只返回业务映射和 Pay 单 ID，不返回渠道秘钥：

```json
{
  "code": 0,
  "data": {
    "merchantOrderId": "CRM-RECEIVABLE-1001",
    "payOrderId": 9001,
    "settlementStatus": "WAITING"
  }
}
```

### 发起已审批客户退款

`POST /admin-api/crm/receivable-refund/payment-refund/create`

请求包含 `refundId` 和 `expectedVersion`。服务端读取金额、来源回款和原支付映射，禁止客户端提交可覆盖
金额、租户或 Pay 单号。响应返回稳定 `merchantRefundId`、`payRefundId` 和结算状态。

### Pay 向 CRM 通知

- `POST /internal-api/crm/payment-notify/order`：`merchantOrderId`、`payOrderId`；
- `POST /internal-api/crm/payment-notify/refund`：`merchantOrderId`、`merchantRefundId`、`payRefundId`。

CRM 接到通知后必须用 Pay API 查询详情，校验应用、租户、商户编号、金额、币种/映射和最终状态，不能
只信任通知体。相同事件重复返回成功；同一映射收到不同 Pay ID 或金额返回冲突并报警。

## 跨进程鉴权头

单体内调用可使用模块 API。若拆分成跨进程 HTTP，除标准追踪头外必须增加：

| Header | 说明 |
|---|---|
| `X-Pay-Event-Id` | 不可复用的事件 ID |
| `X-Pay-Timestamp` | UTC 秒级时间戳 |
| `X-Pay-Nonce` | 128 bit 以上随机值 |
| `X-Pay-Body-SHA256` | 原始请求体摘要 |
| `X-Pay-Signature` | 版本化 HMAC/非对称签名 |
| `X-Tenant-Id` | 路由提示；接收端仍须从 Pay 事实反查并核对 |

签名规范固定为版本、方法、规范化路径、时间戳、nonce、body 摘要的逐行串联。密钥只由 secret 引用加载，
不得进入示例 YAML、日志或数据库普通配置列。

## 错误码

| 错误码 | HTTP | 含义 | 是否重试 |
|---|---:|---|---|
| `PAYX-4001` | 400 | 商户编号格式或请求版本非法 | 否 |
| `PAYX-4002` | 400 | 金额无法无损转换为最小货币单位 | 否 |
| `PAYX-4011` | 401 | 内部事件签名无效 | 否，安全告警 |
| `PAYX-4012` | 401 | 时间窗超限或 nonce/Event ID 重放 | 否，安全告警 |
| `PAYX-4031` | 403 | 当前用户没有业务对象或支付动作权限 | 否 |
| `PAYX-4032` | 403 | Pay 应用、事件与业务对象租户不一致 | 否，安全告警 |
| `PAYX-4041` | 404 | CRM/OA 业务对象不存在或不可见 | 否 |
| `PAYX-4042` | 404 | Pay 单、退款单或映射不存在 | 先对账再决定 |
| `PAYX-4091` | 409 | 商户编号已映射到不同对象或 Pay ID | 否，人工处理 |
| `PAYX-4092` | 409 | 业务审批/状态不允许发起结算 | 否 |
| `PAYX-4093` | 409 | 累计退款超过原成功支付金额 | 否 |
| `PAYX-4221` | 422 | 币种、应用或渠道能力不支持 | 否 |
| `PAYX-5031` | 503 | Pay API/Provider 暂时不可用 | 是，指数退避 |

业务通知接收端只有在本地事务已提交或确认是完全相同的重复事件后返回 `code=0`。未知对象、冲突和校验
失败不得“先成功确认、后人工查”，否则 Pay 通知任务会丢失补偿机会。

