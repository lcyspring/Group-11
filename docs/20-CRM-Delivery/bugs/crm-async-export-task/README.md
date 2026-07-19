# CRM 异步导出任务 Bug 分析与修复日志

日期：2026-07-18。分支：`develop`。状态：已关闭。

| 编号 | 级别 | 现象与根因 | 修复 | 状态 |
|---|---|---|---|---|
| CRM-EXPORT-BUG-001 | P1 | 同步 Excel 导出占用请求线程，大数据量时浏览器等待且无法查看进度 | 改为持久化五态后台任务和独立任务中心 | 已关闭 |
| CRM-EXPORT-BUG-002 | P0 | 仅保存筛选条件会使后台执行时的数据范围漂移 | 提交时同时冻结筛选和已授权对象 ID | 已关闭 |
| CRM-EXPORT-BUG-003 | P0 | 只在提交时鉴权，权限撤回后仍可能生成或下载历史结果 | 生成、令牌签发和下载阶段重复校验对象及权限 | 已关闭 |
| CRM-EXPORT-BUG-004 | P0 | 长期下载地址可转发和重放 | 使用短期随机令牌，只存 SHA-256 并原子单次消费 | 已关闭 |
| CRM-EXPORT-BUG-005 | P1 | 导出结果进入普通文件路径会扩大公共文件路由暴露面 | 结果固定写入 `crm-protected/export/{taskId}` | 已关闭 |
| CRM-EXPORT-BUG-006 | P1 | 多实例并发调度可能重复领取，用户连续点击也可能突破容量检查 | 状态条件更新、Redisson 调度锁和用户提交锁共同保护 | 已关闭 |
| CRM-EXPORT-BUG-007 | P1 | 过期任务若只改状态会遗留敏感 Excel 文件 | 过期扫描先删除受保护文件，再原子标记 `EXPIRED` | 已关闭 |
| CRM-EXPORT-BUG-008 | P0 | 多租户 Job 的 `parallelStream` 工作线程未继承 Spring Boot 类加载器，嵌套 JAR 内 `area.csv` 不可见，任务永久停在运行中 | `TenantJobAspect` 显式传播并恢复应用上下文类加载器；任务消费同时捕获 `LinkageError` 并落为失败态 | 已关闭 |
| CRM-EXPORT-BUG-009 | P1 | HTTP 验收按租户查询 `infra_file`，但该全局表没有 `tenant_id`，导致过期文件断言前脚本失败 | 清理与断言按受保护文件 URL 精确定位，不再假设全局表含租户列 | 已关闭 |

自动化和真实 HTTP 证据位于 `docs/20-CRM-Delivery/testing/crm-async-export-task/`。HTTP 验收已确认
跨用户拒绝、首次下载成功、同令牌重放拒绝、权限撤回后拒绝、过期转态及受保护文件删除。
