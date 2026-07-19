# Bug 文档导航

本目录保存当前开发发现的缺陷总表和独立修复日志。日志记录现象、根因、修复、回归和遗留边界；
历史日志保留当时使用的配置格式与脚本名称，不因后续 KDL/脚本迁移而改写事实。

- [CRM 核心 Bug 总表](CRM-Core-Bug-Report.md)
- [CRM 统计 Bug 总表](CRM-Statistics-Bug-Report.md)
- `logs/`：每个关键 Bug 的独立日志；
- `crm-async-export-task/`、`crm-customer-import-preview/`、`crm-marketing-link-click/`：对应专项缺陷。

本轮文档与技术栈清理记录：
[DOC-TECH-BUG-001](logs/DOC-TECH-BUG-001-stale-configuration-and-toolchain-guidance.md)。

首页公告跳转 404 修复记录：
[CRM-HOME-BUG-002](logs/CRM-HOME-BUG-002-notice-navigation-404.md)。

演示数据财务负责人、客服处理组、竞争对手和 ERP 映射缺失修复记录：
[DEMO-DATA-BUG-008](logs/DEMO-DATA-BUG-008-finance-service-competitor-erp-coverage.md)。

产品分类 CRUD 类型、异常边界与源码杂项清理记录：
[CRM-PRODUCT-BUG-003](logs/CRM-PRODUCT-BUG-003-category-crud-maintainability.md)。

OA 日程创建入口与标准弹窗修复记录：
[OA-EVENT-BUG-004](logs/OA-EVENT-BUG-004-create-entry-no-response.md)。

OA 日程清空日期后查询参数非法修复记录：
[OA-EVENT-BUG-006](logs/OA-EVENT-BUG-006-empty-date-filter.md)。

OA 日程响应时间戳直接显示修复记录：
[OA-EVENT-BUG-007](logs/OA-EVENT-BUG-007-response-time-format.md)。

OA 任务截止时间响应标准化修复记录：
[OA-TASK-BUG-001](logs/OA-TASK-BUG-001-response-time-format.md)。

OA 请示状态、信息和生命周期操作修复记录：
[OA-WORK-REQUEST-BUG-009](logs/OA-WORK-REQUEST-BUG-009-status-and-lifecycle-ui.md)。

CRM 客户拜访返回缓存列表不刷新修复记录：
[CRM-CUSTOMER-VISIT-BUG-001](logs/CRM-CUSTOMER-VISIT-BUG-001-cached-list-not-refreshed.md)。

公共保存成功提示翻译键修复记录：
[WEB-I18N-BUG-005](logs/WEB-I18N-BUG-005-common-save-success-key.md)。

OA 请假列表流程实例翻译路径修复记录：
[OA-LEAVE-BUG-005](logs/OA-LEAVE-BUG-005-process-instance-locale-path.md)。

CRM 确认框取消与业务请求异常边界修复记录：
[CRM-FE-BUG-007](logs/CRM-FE-BUG-007-dialog-errors-silently-swallowed.md)。

Deno/Vite 8 工具链迁移缺陷：
[WEB-BUILD-BUG-003](logs/WEB-BUILD-BUG-003-node-pnpm-toolchain-drift.md)、
[004](logs/WEB-BUILD-BUG-004-deno-lifecycle-node-assumption.md)、
[005](logs/WEB-BUILD-BUG-005-vite8-legacy-build-contracts.md)、
[006](logs/WEB-BUILD-BUG-006-vite8-locale-bundle-verifier.md)、
[007](logs/WEB-BUILD-BUG-007-static-check-false-coverage.md)、
[008](logs/WEB-BUILD-BUG-008-retired-pnpm-cache-residue.md)、
[009](logs/WEB-BUILD-BUG-009-iconify-runtime-remote-fallback.md)。

新增缺陷必须分配稳定编号，修复后记录自动化和人工回归结果；不能只在总表写“已关闭”而没有证据。
