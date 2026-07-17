# CRM 工单手工分派与多维筛选

## 来源与范围

- 原型：`US-WO-002/003`、`PR-WO-001/002`；
- GAP：`GAP-WO-001/002`；
- 复用：现有单人处理人、租户用户、站内信、轨迹表和 RBAC。

本批新增待处理工单的手工重新分派。创建人可分派本人创建的工单；同时具有
`crm:work-order:assign` 与 `crm:work-order:query-all` 的调度人员可分派租户内待处理工单。
目标处理人继续使用现有租户用户，不引入处理组、自动路由或下属组织规则。

## 实现

- `PUT /admin-api/crm/work-order/assign`；
- 新权限 `crm:work-order:assign`，不与修改或处理权限混用；
- 仅状态 10 可分派，更新同时比较旧处理人，避免并发覆盖；
- 禁止把工单重复分派给当前处理人；
- 轨迹动作 7 保存新处理人和可选说明；
- 复用 `crm-work-order-assigned` 模板通知新处理人；
- 列表增加标题、类型、优先级、处理人及“我创建/我处理”筛选；
- 显式场景筛选对 `query-all` 用户仍然生效。

## 后续闭环

本批之后，抄送、处理组、自动分派、移动签到和 SLA 已分别由
`crm-work-order-collaboration` 与 `crm-work-order-service-governance` 完成交付；相关策略使用 YAML 和数据库配置，
不再依赖硬编码。本文保留为早期手工分派批次记录，不再代表工单域当前完成状态。
