# CRM-DATE-BUG-001：非法日期文本被静默写入 1970 年

- 发现日期：2026-07-15
- 分支：`develop`
- 状态：Closed

## 现象

回款验收请求将 `returnTime` 误传为格式化文本时，接口未报参数错误，数据库
反而写入 `1970-01-01 08:00:00`。该回款能完成审批，但不会进入 2026-07 回款
业绩与排行榜，导致业务明细和统计不一致。

## 根因

公共 `TimestampLocalDateTimeDeserializer` 直接使用 `JsonParser.getValueAsLong()`。Jackson
对无法转数字的字符串返回 0，因此非法文本被伪装成了合法的 Unix 起点。

## 修复

- 整数 JSON 令牌使用 `getLongValue()` 精确读取；
- 字符串 JSON 令牌只允许可由 `Long.parseLong()` 解析的毫秒值；
- 非数字字符串抛出 `InvalidFormatException`；
- 其他 JSON 类型抛出 `MismatchedInputException`；
- 保留 Web 日期控件数字字符串契约，不引入前后端破坏性变更。

## 回归结果

- Ubuntu 26.04 专项单测 4/4 通过；
- 目标类 JaCoCo 指令、分支、行、方法覆盖率均为 100%；
- CRM 全量回归 264/264 通过，行覆盖率 41.05%；
- 真实接口对非法文本返回业务码 `400`；
- 毫秒数字字符串继续可用；
- 本轮误建样本精确纠正 1 行，未改动其他业务数据；
- 回款业绩、排行榜和合同累计回款均为 `50.000000`。
