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

Deno/Vite 8 工具链迁移缺陷：
[WEB-BUILD-BUG-003](logs/WEB-BUILD-BUG-003-node-pnpm-toolchain-drift.md)、
[004](logs/WEB-BUILD-BUG-004-deno-lifecycle-node-assumption.md)、
[005](logs/WEB-BUILD-BUG-005-vite8-legacy-build-contracts.md)、
[006](logs/WEB-BUILD-BUG-006-vite8-locale-bundle-verifier.md)、
[007](logs/WEB-BUILD-BUG-007-static-check-false-coverage.md)、
[008](logs/WEB-BUILD-BUG-008-retired-pnpm-cache-residue.md)、
[009](logs/WEB-BUILD-BUG-009-iconify-runtime-remote-fallback.md)。

新增缺陷必须分配稳定编号，修复后记录自动化和人工回归结果；不能只在总表写“已关闭”而没有证据。
