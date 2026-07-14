# CRM-FEATURE-002：客户首联系人生命周期

## 需求证据

- `docs/02-Prototype-Analysis/user-stories/01-Core-Domains.md` 的 `US-CUS-010`：联系人可设首联系人，首联系人不可直接删除，变更可追溯。
- `docs/03-Gap-Analysis/03-Business-Domain-Gaps.md` 的 `GAP-CUS-002`：现有联系人能力缺少首联系人及编辑边界。
- `docs/Proj-Docs-v-6/05-Business-Requirements/02-SRS-User-Stories.md`：每个客户只有一个首要联系人。
- `docs/Proj-Docs-v-6/14-Frontend/01-CRM-Frontend-Design.md`：客户列表展示首联系人和手机号码。

## 语义决策

现有 `crm_contact.master` 的数据库注释、后端注释及中/英/阿文案都表示“关键决策人”，不能复用为“首联系人”。本功能新增独立字段：

- 数据库：`primary_contact`；
- Java/TypeScript：`primaryContact`；
- `master` 保持关键决策人语义，两者可以独立取值。

## 生命周期规则

- 客户的首个联系人无论请求值如何都自动成为首联系人。
- 新建或更新另一联系人为首联系人时，原首联系人自动降级。
- 当前首联系人不能直接取消、删除或移动到另一客户；应先把另一联系人设为首联系人。
- 同一客户的首联系人变更以客户行锁串行化。
- 更新前已存在普通查询快照时，首联系人查询使用 `FOR UPDATE` 当前读，避免 MySQL RR 旧快照。
- 更新和删除等待客户锁后，对联系人本身也使用 `FOR UPDATE` 当前读；若等待期间联系人被移动，返回 `1020003009` 要求重试。
- 取消原首联系人必须恰好更新一行，否则返回冲突错误并回滚。
- 首联系人自动设置及切换说明写入 CRM 联系人操作日志。

## 数据与查询

- 幂等迁移新增 `crm_contact.primary_contact bit(1) NOT NULL DEFAULT 0`。
- 历史数据按租户、客户保留最早的既有首联系人；没有首联系人时选择最早有效联系人。
- 新增 `(tenant_id, customer_id, primary_contact, deleted)` 索引。
- 客户列表/详情使用一次批量查询映射首联系人姓名和手机，避免 N+1。

## 前端范围

- 联系人表单增加独立“首联系人”选择，不替换“关键决策人”。
- 联系人主列表、客户/商机联系人列表、选择弹窗和联系人详情显示首联系人状态。
- 客户主列表显示首联系人姓名和首联系人手机。
- 中、英、阿三语文案同步。

## 完成证据

- 联系人专项单元测试 16/16；CRM 全量自动化 33/33。
- ESLint、Web `build:prod`、Server 生产 JAR 均通过。
- Podman 真实 API 9 项验证通过，包括两个并发切换请求后首联系人数量仍为 1。
- Podman 额外锁等待并发验证 2/2：旧表单更新和旧快照删除均读取最新首联系人状态并被拒绝。
- 生命周期实际产生 6 条联系人操作日志。
- 历史迁移重复执行两次无错误，历史 6 个有联系人的客户均恰好一个首联系人。
- 临时客户、联系人、权限和测试操作日志清理后均为 0。

详细测试与覆盖率见 `docs/20-CRM-Delivery/testing/contact-primary/`。
