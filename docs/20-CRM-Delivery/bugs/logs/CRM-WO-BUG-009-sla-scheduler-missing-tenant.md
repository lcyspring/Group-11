# CRM-WO-BUG-009：SLA 定时扫描缺少租户上下文

- 现象：每个 SLA 扫描周期日志出现 `TenantContextHolder 不存在租户编号`，数据库查询失败；前端在进入 CRM 页面时可能看到“系统异常”。
- 根因：定时器直接调用 `CrmWorkOrderService.processDueSla()`，没有经过 `@TenantJob` 的租户遍历边界。
- 修复：新增 `CrmWorkOrderSlaJob`，由 `@TenantJob` 执行服务；调度器只负责集群锁和触发 Job。
- 验证：新增调度器委托测试和 Job 结果测试；重新构建后确认定时日志不再出现租户上下文异常。
