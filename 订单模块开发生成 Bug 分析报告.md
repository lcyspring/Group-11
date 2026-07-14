
# Day 1 订单模块开发 Bug 报告

**作者**: 刘王子
**日期**: 2026-07-14
**分支**: lwz

---

## 一、Bug概述

本次订单模块开发任务共遇到 **8个Bug**，均已修复。以下按发现时间顺序详细记录每个Bug的发现过程、根因分析、解决方案及验证结果。

---

## 二、Bug统计表

| 编号 | 类型 | 描述 | 解决方案 | 状态 |
|------|------|------|----------|------|
| BUG-001 | 数据模型缺失 | `TradeOrderDO` 缺少 `items` 字段，无法关联订单产品行 | 添加 `@TableField(exist=false) List<TradeOrderItemDO> items` | ✅ 已修复 |
| BUG-002 | 导入路径错误 | ServiceImpl、Controller、Test 类引用错误的包路径 | 更新导入路径为 `com.meession.etm.module.trade.dal.dataobject.order` | ✅ 已修复 |
| BUG-003 | Mapper方法缺失 | `TradeOrderMapper` 缺少分页查询和用户订单查询方法 | 添加 `selectPage()` 和 `selectListByUserId()` 方法 | ✅ 已修复 |
| BUG-004 | Lambda变量问题 | 测试中 lambda 内部引用的变量必须是 final | 将 `i % 2` 赋值给 final 变量后再使用 | ✅ 已修复 |
| BUG-005 | 数据库字段缺失 | H2 测试数据库缺少 `give_coupon_template_counts` 等字段 | 更新 `create_tables.sql` 添加缺失字段 | ✅ 已修复 |
| BUG-006 | 字段约束冲突 | `point_price` 字段不允许 NULL，测试数据未设置该值 | 在测试中设置 `pointPrice=0` | ✅ 已修复 |
| BUG-007 | 方法重载歧义 | `selectPage()` 方法调用存在歧义 | 显式转换参数类型 `(Set<Long>) null` | ✅ 已修复 |
| BUG-008 | VO字段缺失 | `TradeOrderSaveReqVO` 缺少 `pointPrice` 字段 | 添加 `private Integer pointPrice` 字段 | ✅ 已修复 |

---

## 三、Bug详细记录

### Bug 01: 数据模型缺失 - TradeOrderDO缺少items字段

| 属性 | 值 |
|------|------|
| **编号** | BUG-001 |
| **严重程度** | 高 |
| **状态** | ✅ 已修复 |
| **发现阶段** | 编译阶段 |

#### 问题描述

查询订单详情时无法获取关联的订单产品行数据，因为 `TradeOrderDO` 未定义 `items` 字段。

#### 根因分析

订单主表和订单产品行表是一对多关系，`TradeOrderDO` 需要一个非数据库字段来存储关联的产品行列表，但原始代码中缺少该字段。

#### 解决方案

在 `TradeOrderDO` 中添加非数据库映射字段：

```java
@TableField(exist = false)
private List<TradeOrderItemDO> items;
```

#### 修改文件

| 文件路径 | 修改内容 |
|---------|---------|
| `dal/dataobject/order/TradeOrderDO.java` | 第363行：添加 `items` 字段及 `@TableField(exist=false)` 注解 |

#### 验证结果

```
测试通过：订单详情接口成功返回订单及其产品行列表
```

---

### Bug 02: 导入路径错误 - ServiceImpl/Controller/Test引用错误包

| 属性 | 值 |
|------|------|
| **编号** | BUG-002 |
| **严重程度** | 高 |
| **状态** | ✅ 已修复 |
| **发现阶段** | 编译阶段 |

#### 问题描述

编译 `mitedtsm-module-trade` 模块时，出现大量 `cannot find symbol` 错误：

```
error: cannot find symbol
  symbol: class TradeOrderDO
  location: class TradeOrderServiceImpl
```

涉及文件：
- `TradeOrderServiceImpl.java`
- `TradeOrderController.java`
- `TradeOrderServiceTest.java`

#### 根因分析

DO 和 Mapper 类位于 `com.meession.etm.module.trade.dal.dataobject.order` 和 `com.meession.etm.module.trade.dal.mysql.order` 包下，但导入语句引用了错误的包路径。

#### 解决方案

更新所有导入语句为正确的包路径：

```java
// 修改前
import com.meession.etm.module.trade.dal.dataobject.TradeOrderDO;

// 修改后
import com.meession.etm.module.trade.dal.dataobject.order.TradeOrderDO;
```

#### 修改文件

| 文件路径 | 修改内容 |
|---------|---------|
| `service/TradeOrderServiceImpl.java` | 更新 DO 和 Mapper 的导入路径 |
| `controller/admin/order/TradeOrderController.java` | 更新 DO 和 VO 的导入路径 |
| `test/java/com/meession/etm/module/trade/service/TradeOrderServiceTest.java` | 更新 DO 和 Mapper 的导入路径 |

#### 验证结果

```
mvn compile -pl mitedtsm-module-mall/mitedtsm-module-trade -am -q
# BUILD SUCCESS
```

---

### Bug 03: Mapper方法缺失 - TradeOrderMapper缺少查询方法

| 属性 | 值 |
|------|------|
| **编号** | BUG-003 |
| **严重程度** | 高 |
| **状态** | ✅ 已修复 |
| **发现阶段** | 编译阶段 |

#### 问题描述

编译错误提示缺少以下方法：
- `TradeOrderMapper.selectPage(TradeOrderPageReqVO)`
- `TradeOrderMapper.selectListByUserId(Long)`
- `TradeOrderItemMapper.deleteByOrderId(Long)`

#### 根因分析

Service 层调用了 Mapper 中未定义的方法，需要添加对应的 default 方法实现。

#### 解决方案

在 Mapper 接口中添加缺失的方法：

```java
// TradeOrderMapper.java
default PageResult<TradeOrderDO> selectPage(TradeOrderPageReqVO reqVO) {
    return selectPage(reqVO, (Set<Long>) null);
}

default List<TradeOrderDO> selectListByUserId(Long userId) {
    return selectList(TradeOrderDO::getUserId, userId);
}

// TradeOrderItemMapper.java
default int deleteByOrderId(Long orderId) {
    return delete(TradeOrderItemDO::getOrderId, orderId);
}
```

#### 修改文件

| 文件路径 | 修改内容 |
|---------|---------|
| `dal/mysql/order/TradeOrderMapper.java` | 添加 `selectPage()` 和 `selectListByUserId()` 方法 |
| `dal/mysql/order/TradeOrderItemMapper.java` | 添加 `deleteByOrderId()` 方法 |

#### 验证结果

```
编译通过，测试用例可正常调用相关方法
```

---

### Bug 04: Lambda变量问题 - 测试中变量必须是final

| 属性 | 值 |
|------|------|
| **编号** | BUG-004 |
| **严重程度** | 中 |
| **状态** | ✅ 已修复 |
| **发现阶段** | 编译阶段 |

#### 问题描述

测试类编译错误：

```
local variable i defined in an enclosing scope must be final or effectively final
```

涉及文件：`TradeOrderServiceTest.java` 第140行附近

#### 根因分析

Java 8+ 要求 lambda 表达式中引用的局部变量必须是 final 或 effectively final，而测试中在 for 循环内的 lambda 中直接使用了循环变量 `i`。

#### 解决方案

将循环变量赋值给 final 变量后再使用：

```java
// 修改前
for (int i = 0; i < 5; i++) {
    TradeOrderDO order = randomPojo(TradeOrderDO.class, o -> {
        o.setStatus(i % 2);  // 错误：i 不是 final
    });
}

// 修改后
for (int i = 0; i < 5; i++) {
    final int status = i % 2;
    TradeOrderDO order = randomPojo(TradeOrderDO.class, o -> {
        o.setStatus(status);  // 正确：status 是 final
    });
}
```

#### 修改文件

| 文件路径 | 修改内容 |
|---------|---------|
| `test/java/com/meession/etm/module/trade/service/TradeOrderServiceTest.java` | 第140行：将 `i % 2` 赋值给 final 变量 |

#### 验证结果

```
测试类编译通过
```

---

### Bug 05: 数据库字段缺失 - H2测试数据库缺少字段

| 属性 | 值 |
|------|------|
| **编号** | BUG-005 |
| **严重程度** | 高 |
| **状态** | ✅ 已修复 |
| **发现阶段** | 测试执行阶段 |

#### 问题描述

运行测试时出现数据库错误：

```
Column 'give_coupon_template_counts' not found; SQL statement:
INSERT INTO trade_order (...) VALUES (...)
```

#### 根因分析

`TradeOrderDO` 中定义了 `giveCouponTemplateCounts`、`giveCouponIds`、`pointActivityId` 等字段，但测试用的 H2 数据库建表脚本 `create_tables.sql` 中缺少这些字段定义。

#### 解决方案

更新 `create_tables.sql`，添加缺失的字段：

```sql
-- trade_order 表中添加
give_coupon_template_counts varchar NULL,
give_coupon_ids             varchar NULL,
point_activity_id           bigint  NULL,
```

同时更新 `pick_up_store_id`、`seckill_activity_id` 等字段类型为 `bigint`（原为 `long`）以兼容 H2 语法。

#### 修改文件

| 文件路径 | 修改内容 |
|---------|---------|
| `test/resources/sql/create_tables.sql` | 完善 `trade_order` 和 `trade_order_item` 表结构 |

#### 验证结果

```
测试运行时不再报字段缺失错误
```

---

### Bug 06: 字段约束冲突 - point_price不允许NULL

| 属性 | 值 |
|------|------|
| **编号** | BUG-006 |
| **严重程度** | 中 |
| **状态** | ✅ 已修复 |
| **发现阶段** | 测试执行阶段 |

#### 问题描述

测试报错：

```
NULL not allowed for column "point_price"; SQL statement:
INSERT INTO trade_order (...) VALUES (...)
```

#### 根因分析

数据库表中 `point_price` 字段定义为 `NOT NULL`，但测试用例创建订单时未设置该字段值，导致插入失败。

#### 解决方案

在测试用例中显式设置 `pointPrice`、`couponId`、`couponPrice` 等必填字段：

```java
createReqVO.setCouponId(0L);
createReqVO.setCouponPrice(0);
createReqVO.setPointPrice(0);
```

#### 修改文件

| 文件路径 | 修改内容 |
|---------|---------|
| `test/java/com/meession/etm/module/trade/service/TradeOrderServiceTest.java` | 创建和更新订单测试中添加必填字段 |

#### 验证结果

```
订单创建测试通过
```

---

### Bug 07: 方法重载歧义 - selectPage方法调用歧义

| 属性 | 值 |
|------|------|
| **编号** | BUG-007 |
| **严重程度** | 中 |
| **状态** | ✅ 已修复 |
| **发现阶段** | 编译阶段 |

#### 问题描述

编译错误：

```
The method selectPage(TradeOrderPageReqVO, Set<Long>) is ambiguous 
for the type TradeOrderMapper
```

#### 根因分析

`selectPage()` 方法有两个重载版本，调用时传入 `null` 参数无法确定调用哪个版本。

#### 解决方案

在 default 方法中显式转换参数类型：

```java
default PageResult<TradeOrderDO> selectPage(TradeOrderPageReqVO reqVO) {
    return selectPage(reqVO, (Set<Long>) null);  // 显式转换
}
```

#### 修改文件

| 文件路径 | 修改内容 |
|---------|---------|
| `dal/mysql/order/TradeOrderMapper.java` | 第32行：添加显式类型转换 |

#### 验证结果

```
编译通过
```

---

### Bug 08: VO字段缺失 - TradeOrderSaveReqVO缺少pointPrice字段

| 属性 | 值 |
|------|------|
| **编号** | BUG-008 |
| **严重程度** | 中 |
| **状态** | ✅ 已修复 |
| **发现阶段** | 编译阶段 |

#### 问题描述

测试类编译错误：

```
The method setPointPrice(int) is undefined for the type TradeOrderSaveReqVO
```

#### 根因分析

测试用例中调用了 `setPointPrice()` 方法，但 `TradeOrderSaveReqVO` 类中未定义该字段。

#### 解决方案

在 `TradeOrderSaveReqVO` 中添加 `pointPrice` 字段：

```java
private Integer pointPrice;
```

#### 修改文件

| 文件路径 | 修改内容 |
|---------|---------|
| `controller/admin/order/vo/TradeOrderSaveReqVO.java` | 第85行：添加 `pointPrice` 字段 |

#### 验证结果

```
编译通过，测试正常运行
```

---

## 四、测试验证结果

修复所有Bug后，运行订单模块单元测试：

```
Tests run: 16, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

### 测试覆盖范围

| 测试类别 | 测试方法数 | 通过率 |
|---------|-----------|--------|
| 订单创建 | 1 | 100% |
| 订单更新 | 2 | 100% |
| 订单删除 | 3 | 100% |
| 订单查询 | 4 | 100% |
| 订单产品行 | 3 | 100% |
| 订单状态更新 | 2 | 100% |

---

## 五、总结

本次订单模块开发共修复 **8个Bug**，涵盖以下类型：

- **数据模型问题**：2个（字段缺失、类型定义）
- **编译错误**：3个（导入路径、方法缺失、方法歧义）
- **测试数据问题**：2个（必填字段未设置、字段类型不匹配）
- **语法规范问题**：1个（Lambda变量必须final）

所有Bug均已通过编译验证和单元测试验证，代码已提交至 `lwz` 分支。
