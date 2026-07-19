# CRM 客户关怀测试结果

日期：2026-07-18。环境：Ubuntu 26.04 构建容器、rootless Podman 运行服务。

| 检查 | 结果 |
|---|---|
| Server package | 通过 |
| CRM 全量自动化 | 533/533，通过，失败 0、错误 0、跳过 0 |
| 客户关怀服务与调度专项 | 19/19，通过 |
| Web 客户关怀专项 | 8/8，通过 |
| Web 行/分支/函数覆盖率 | 100% / 100% / 100% |
| Web ESLint | 零警告 |
| Web production build | 通过 |
| 真实计划维护 | 创建、详情、编辑、启停、启用删除拒绝、停用删除通过 |
| 真实记录与生日查询 | 数据范围、可读名称、近期生日通过 |
| 成交后回访契约 | 规则类型、回访天数、目标范围通过 |
| 验收数据清理 | 计划、记录 0 条残留，联系人生日已恢复 |
| 客户与联系人生日维护修复 | Ubuntu CRM 533/533；前端生日来源专项与关怀契约 6/6 |
| 生日查询口径 | 显式区分客户自身生日与联系人生日；两类新增/编辑、列表、详情和三语文案已补齐 |

运行入口：

```bash
bash ./podman/compile.sh ./podman/config/verify-crm-customer-care-ubuntu-26.04.kdl
bash ./podman/compile.sh ./podman/config/verify-crm-customer-care-web-ubuntu-26.04.kdl
bash ./podman/tests/acceptance/verify-crm-customer-care.sh ./podman/config/verify-crm-customer-care-local.kdl
```
