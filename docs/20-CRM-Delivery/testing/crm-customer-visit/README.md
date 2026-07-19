# CRM 客户拜访测试记录

| 层级 | 结果 | 关键覆盖 |
|---|---|---|
| `CrmCustomerVisitServiceImplTest` | 4/4 | 客户联系人匹配、受管流程、审批条件、一次性结果和跟进联动 |
| CRM 模块回归 | 541/541 | 全量 Surefire，0 失败、0 错误、0 跳过 |
| Web 契约 | 4/4 | 创建、流程定义、审批进度、结果入口、隐藏路由、返回缓存列表即时刷新 |
| Web 专项覆盖率 | 100% | 行、分支、函数；范围为客户拜访契约测试 |
| Production 构建 | 通过 | Ubuntu 26.04 公共工具链，`image.rebuild: false` |

2026-07-19 使用 `verify-crm-customer-visit-ubuntu-26.04.kdl` 重跑：客户拜访契约 4/4、公共保存提示 2/2、CRM 541/541，ESLint 0 warning，Vite 8 生产构建及三语言包完整性通过。生成 `coverage/verify-crm-customer-visit.lcov`；专项实际加载模块为 100%，不把静态 SFC 契约误报为浏览器运行时覆盖率。
