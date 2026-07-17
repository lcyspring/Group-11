# CRM 客户拜访测试记录

| 层级 | 结果 | 关键覆盖 |
|---|---|---|
| `CrmCustomerVisitServiceImplTest` | 4/4 | 客户联系人匹配、受管流程、审批条件、一次性结果和跟进联动 |
| CRM 模块回归 | 490/490 | 全量 Surefire，0 失败、0 错误、0 跳过 |
| Web 契约 | 3/3 | 创建、流程定义、审批进度、结果入口、隐藏路由 |
| Web 专项覆盖率 | 100% | 行、分支、函数；范围为客户拜访契约测试 |
| Production 构建 | 通过 | Ubuntu 26.04 公共工具链，`image.rebuild: false` |
