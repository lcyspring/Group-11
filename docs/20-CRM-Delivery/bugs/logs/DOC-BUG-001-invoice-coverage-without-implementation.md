# DOC-BUG-001：原测试模板将未实现发票标为已覆盖

- 发现日期：2026-07-14
- 级别：P1 / 交付状态误报
- 状态：已在交付台账纠正，原始基线文档保留证据

## 现象

`docs/Proj-Docs-v-6/10-Testing/04-Coverage-Report.md` 将 `REQ-PAY-003 发票管理` 标记为
“已覆盖”，但仓库中不存在 CRM Invoice Controller、Service、DO、Mapper、前端页面或
`crm_invoice` 表。该文件同时保留大量空白覆盖率占位符，应视为测试报告模板，而不是实测结果。

## 处理

不篡改项目基线文档；在 `docs/20-CRM-Delivery/planning/crm-finance-implementation-status/`
建立基于代码、表和测试证据的状态表。2026-07-14 后续已按 ADR-009 实现真实发票聚合、MySQL
迁移、前端和专项测试；新证据位于 `features/crm-invoice-lifecycle/` 与
`testing/crm-invoice-lifecycle/`，不再沿用原模板的“已覆盖”结论。
