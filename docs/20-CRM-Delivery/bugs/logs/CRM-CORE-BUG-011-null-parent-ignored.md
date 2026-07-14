# CRM-CORE-BUG-011：解除上级客户时接口成功但关系未清空

发现/关闭日期：2026-07-13
级别：P1
关联差异：`GAP-CUS-002`

## 现象

把客户的 `parentCustomerId` 更新为 `null` 时接口返回成功，但数据库仍保留旧的 `parent_customer_id`。随后删除原父客户仍返回“有关联下级客户”。

## 根因

通用 `updateById` 遵循 MyBatis 默认非空字段策略，Java 对象中的 `null` 不会生成 `SET parent_customer_id = NULL`。这对多数可选字段可避免误覆盖，但使“显式解除父关系”和“未传该字段”无法仅靠通用更新区分。

不能把 DO 字段简单配置为全局 `ALWAYS`：成交状态、跟进、负责人等大量局部 `updateById(new CrmCustomerDO().setId(...))` 会携带空父字段，从而在无关业务操作中误清空层级。

## 修复

- 保留通用客户字段更新策略。
- 新增 `updateParentCustomerIdById(id, parentCustomerId)`，通过更新 Wrapper 显式生成父字段 SET，允许值为 NULL。
- 客户完整编辑在层级锁和校验后调用该专用更新。

## 验证

- 新增单元回归，断言 Service 明确调用父关系更新且传入 NULL。
- 真实 API 清空后直接查询 MySQL，字段为 `NULL`。
- 原父客户随后可删除，证明删除保护读取到的关系已解除。
- CRM 全量回归 50/50。

## 状态

已关闭。
