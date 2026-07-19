# 订单模块 API 接口文档

**版本**: v1.0  
**更新日期**: 2026-07-18  
**作者**: 刘王子  

---

## 一、模块概述

订单模块提供订单全生命周期管理能力，包括订单创建、状态流转、审批集成、财务集成、合同管理等核心功能。

---

## 二、接口列表

### 2.1 订单基础接口

| 接口 | 方法 | 路径 | 说明 |
|------|------|------|------|
| 创建订单 | POST | `/trade/order/create` | 创建新订单 |
| 更新订单 | PUT | `/trade/order/update` | 更新订单信息 |
| 删除订单 | DELETE | `/trade/order/delete?id={id}` | 删除订单 |
| 获取订单详情 | GET | `/trade/order/get-detail?id={id}` | 获取订单详情 |
| 分页查询订单 | GET | `/trade/order/page` | 分页查询订单列表 |

### 2.2 订单状态接口

| 接口 | 方法 | 路径 | 说明 |
|------|------|------|------|
| 更新订单状态 | POST | `/trade/order/update-status` | 更新订单状态（状态机校验） |
| 取消订单 | POST | `/trade/order/cancel?id={id}` | 取消订单 |

### 2.3 订单审批接口

| 接口 | 方法 | 路径 | 说明 |
|------|------|------|------|
| 提交审批 | POST | `/trade/order/approval/submit` | 提交订单审批 |
| 取消审批 | POST | `/trade/order/approval/cancel?id={id}` | 取消订单审批 |
| 获取审批状态 | GET | `/trade/order/approval/status?id={id}` | 获取审批状态 |

### 2.4 订单财务接口

| 接口 | 方法 | 路径 | 说明 |
|------|------|------|------|
| 处理支付 | POST | `/trade/order/finance/payment` | 处理订单支付 |
| 处理退款 | POST | `/trade/order/finance/refund` | 处理订单退款 |
| 获取支付记录 | GET | `/trade/order/finance/payment/{paymentId}` | 获取支付记录 |
| 获取订单支付记录 | GET | `/trade/order/finance/payment/order/{orderId}` | 获取订单支付记录列表 |

### 2.5 订单合同接口

| 接口 | 方法 | 路径 | 说明 |
|------|------|------|------|
| 创建合同 | POST | `/trade/order/contract/create` | 为订单创建合同 |
| 绑定合同 | POST | `/trade/order/contract/bind` | 绑定合同到订单 |
| 获取订单合同 | GET | `/trade/order/contract/order/{orderId}` | 根据订单ID获取合同 |
| 签署合同 | POST | `/trade/order/contract/sign` | 签署合同 |

### 2.6 订单报表接口

| 接口 | 方法 | 路径 | 说明 |
|------|------|------|------|
| 汇总报表 | GET | `/trade/order/report/summary` | 获取订单汇总报表 |
| 日报表 | GET | `/trade/order/report/daily` | 获取订单日报表 |

### 2.7 订单分析接口

| 接口 | 方法 | 路径 | 说明 |
|------|------|------|------|
| 数据分析 | GET | `/trade/order/analysis/summary` | 获取订单数据分析 |

---

## 三、接口详细说明

### 3.1 创建订单

**请求路径**: `POST /trade/order/create`

**请求参数**:

```json
{
  "userId": 1,
  "type": 0,
  "terminal": 1,
  "userIp": "127.0.0.1",
  "receiverName": "张三",
  "receiverMobile": "13800138000",
  "receiverAreaId": 110101,
  "receiverDetailAddress": "北京市东城区XX街道XX号",
  "deliveryType": 1,
  "items": [
    {
      "productId": 1,
      "skuId": 1,
      "count": 2
    }
  ],
  "couponId": null,
  "remark": "备注信息"
}
```

**响应结果**:

```json
{
  "code": 0,
  "data": 1001,
  "msg": "success"
}
```

### 3.2 更新订单状态

**请求路径**: `POST /trade/order/update-status`

**请求参数**:

```json
{
  "orderId": 1001,
  "status": 20,
  "remark": "用户已支付"
}
```

**状态机规则**:

| 当前状态 | 允许转换状态 |
|---------|-------------|
| 待付款(10) | 已付款(20)、已取消(50) |
| 已付款(20) | 待发货(30)、已取消(50)、退款中(60) |
| 待发货(30) | 已收货(40)、退款中(60) |
| 已收货(40) | 退款中(60)、已完成(50) |
| 退款中(60) | 已退款(70) |

### 3.3 处理支付

**请求路径**: `POST /trade/order/finance/payment`

**请求参数**:

```json
{
  "orderId": 1001,
  "payType": 1,
  "payAmount": 10000,
  "payChannel": "wechat",
  "transactionNo": "T123456789",
  "remark": "微信支付"
}
```

**响应结果**:

```json
{
  "code": 0,
  "data": 10001,
  "msg": "success"
}
```

### 3.4 创建合同

**请求路径**: `POST /trade/order/contract/create`

**请求参数**:

```
orderId=1001&contractName=销售合同&attachmentUrls=http://xxx/contract.pdf
```

**响应结果**:

```json
{
  "code": 0,
  "data": 2001,
  "msg": "success"
}
```

### 3.5 获取订单详情

**请求路径**: `GET /trade/order/get-detail?id=1001`

**响应结果**:

```json
{
  "code": 0,
  "data": {
    "id": 1001,
    "no": "O202401010001",
    "type": 0,
    "status": 20,
    "statusName": "已付款",
    "userId": 1,
    "userName": "张三",
    "userMobile": "13800138000",
    "productCount": 2,
    "totalPrice": 10000,
    "totalPriceYuan": 100.00,
    "payPrice": 10000,
    "payPriceYuan": 100.00,
    "payStatus": true,
    "payType": 1,
    "payTypeName": "微信支付",
    "payTime": "2024-01-01 10:00:00",
    "receiverName": "张三",
    "receiverMobile": "13800138000",
    "receiverAddress": "北京市东城区XX街道XX号",
    "items": [
      {
        "id": 1,
        "productId": 1,
        "productName": "商品名称",
        "productImage": "http://xxx/image.jpg",
        "skuId": 1,
        "skuName": "规格名称",
        "count": 2,
        "price": 5000,
        "payPrice": 10000
      }
    ],
    "contractInfo": {
      "contractId": 2001,
      "contractNo": "C202401010001",
      "contractName": "销售合同",
      "contractStatus": 1,
      "contractStatusName": "待签署"
    },
    "paymentInfo": {
      "paymentId": 10001,
      "payType": 1,
      "payChannel": "wechat",
      "payAmount": 10000,
      "payTime": "2024-01-01 10:00:00",
      "transactionNo": "T123456789"
    },
    "createTime": "2024-01-01 09:00:00"
  },
  "msg": "success"
}
```

---

## 四、错误码说明

| 错误码 | 说明 |
|-------|------|
| 1003000001 | 订单不存在 |
| 1003000002 | 订单项不存在 |
| 1003000003 | 订单状态不正确 |
| 1003000008 | 订单状态不允许转换 |
| 1003000009 | 支付金额不匹配 |
| 1003000010 | 支付金额不足 |
| 1003001001 | 合同不存在 |
| 1003001002 | 合同状态不正确 |
| 1003002001 | 商机不存在 |
| 1003002004 | 商机无法转换为订单 |

---

## 五、调用示例

### 5.1 cURL 示例

```bash
# 创建订单
curl -X POST 'http://localhost:8080/trade/order/create' \
  -H 'Content-Type: application/json' \
  -H 'userId: 1' \
  -d '{
    "userId": 1,
    "type": 0,
    "terminal": 1,
    "userIp": "127.0.0.1",
    "receiverName": "张三",
    "receiverMobile": "13800138000",
    "receiverAreaId": 110101,
    "receiverDetailAddress": "北京市东城区",
    "deliveryType": 1,
    "items": [{"productId": 1, "skuId": 1, "count": 2}]
  }'

# 处理支付
curl -X POST 'http://localhost:8080/trade/order/finance/payment' \
  -H 'Content-Type: application/json' \
  -H 'userId: 1' \
  -d '{
    "orderId": 1001,
    "payType": 1,
    "payAmount": 10000,
    "payChannel": "wechat"
  }'
```

### 5.2 Java 示例

```java
// 创建订单
TradeOrderCreateReqVO reqVO = new TradeOrderCreateReqVO();
reqVO.setUserId(1L);
reqVO.setType(0);
reqVO.setTerminal(1);
reqVO.setUserIp("127.0.0.1");
reqVO.setReceiverName("张三");
reqVO.setReceiverMobile("13800138000");

List<TradeOrderCreateReqVO.OrderItem> items = new ArrayList<>();
TradeOrderCreateReqVO.OrderItem item = new TradeOrderCreateReqVO.OrderItem();
item.setProductId(1L);
item.setSkuId(1L);
item.setCount(2);
items.add(item);
reqVO.setItems(items);

Long orderId = tradeOrderService.createOrder(reqVO);
```

---

## 六、注意事项

1. **状态转换**: 订单状态转换必须遵守状态机规则，否则返回错误
2. **支付金额**: 支付金额必须等于订单应付金额，不支持部分支付
3. **审批流程**: 订单创建后需要提交审批，审批通过后才能进行后续操作
4. **合同管理**: 只有已付款的订单才能创建合同
5. **缓存策略**: 订单详情使用两级缓存（本地缓存+Redis），更新时自动清除
6. **分布式锁**: 订单状态更新使用分布式锁保证并发安全