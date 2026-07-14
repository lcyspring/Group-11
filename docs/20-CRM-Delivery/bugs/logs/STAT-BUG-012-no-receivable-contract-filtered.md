# STAT-BUG-012：无回款合同被客户转化统计过滤

## 基本信息

- 发现日期：2026-07-13
- 级别：P1
- 模块：CRM / 数据统计 / 客户转化率
- 状态：已关闭
- 影响接口：`GET /admin-api/crm/statistics-customer/get-contract-summary`

## 现象与影响

审批通过但尚未产生回款记录的合同不会出现在客户转化率明细中。结果会系统性漏算新成交、未回款客户，使转化明细和合同台账不一致。

## 根因

查询先对 `crm_receivable` 做 `LEFT JOIN`，随后又在 `WHERE` 中要求 `receivable.deleted = 0`。当合同没有任何回款行时，回款字段为 `NULL`，该条件不成立，`LEFT JOIN` 实际退化为内连接。

## 修复关键

- 将回款逻辑删除条件移动到 `LEFT JOIN ... ON` 子句。
- `WHERE` 仅保留客户、合同自身的删除和审批条件。
- 保留 `IFNULL(receivable.price, 0)`，无回款合同行明确返回零金额。

代码位置：

- `Server/mitedtsm-module-crm/src/main/resources/mapper/statistics/CrmStatisticsCustomerMapper.xml`

## 验证证据

1. Maven CRM 统计专项测试：6/6 通过。
2. Server 完整生产 JAR 构建：通过。
3. 使用最新 JAR 重建 rootless Podman，Spring Boot、Web、Mall 均健康。
4. 创建临时客户和审批通过合同，合同金额为 `888.00`，不创建任何回款记录。
5. 调用转化统计接口，返回：
   - `customerName=STAT-NO-RECEIVABLE-20260713`
   - `contractName=STAT-NO-RECEIVABLE-CONTRACT`
   - `totalPrice=888.00`
   - `receivablePrice=0.000000`
6. 临时客户、合同和合同产品记录已物理清理，三张表残留数均为 0。

## 遗留风险

当前查询仍按回款明细行连接；一份合同存在多笔回款时可能返回多行合同或产生前端重复展示。该问题应作为独立 Bug 审计，不与本缺陷合并关闭。
