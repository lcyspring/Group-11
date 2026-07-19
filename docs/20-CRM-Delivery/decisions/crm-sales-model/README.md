# CRM 销售模型 ADR 决策包

状态：Accepted（MVP）
批准日期：2026-07-14
批准人：项目负责人（本轮明确回复“可以”）
实施分支：`develop`

本目录关闭销售主链的 `ADR-002`～`ADR-006` 决策门。冻结后的 MVP 主链为：

```text
商机报价 → 赢单 → 显式创建 CRM 合同 → crm-contract-audit → 回款计划 → 回款
```

ERP 履约订单、独立 CRM 销售订单、统一 PIM 和完整审批撤回/重开能力属于后续演进，
不阻塞当前 CRM 主闭环，但必须通过公开接口和新的 ADR 变更进入。

| ADR | 结论 | 状态 |
|---|---|---|
| ADR-002 | CRM 合同是 MVP 正式销售协议，不新建重复 CRM 销售订单 | Accepted |
| ADR-003 | CRM 产品目录是 MVP 报价来源，合同保存交易产品行快照 | Accepted |
| ADR-004 | 赢单后由用户显式、幂等创建 CRM 合同，不自动创建 ERP 订单 | Accepted |
| ADR-005 | 合同初始继承商机客户与负责人，绩效按合同负责人 | Accepted |
| ADR-006 | 复用 `crm-contract-audit`，业务状态与流程状态分离 | Accepted for MVP |
