# 可回款合同候选测试结果

- 日期：2026-07-16
- 环境：Ubuntu 26.04 rootless Podman
- 状态：通过

| 检查项 | 结果 |
|---|---|
| 普通用户 OWNER/WRITE 过滤 | 通过 |
| READ-only 排除 | 通过 |
| CRM 管理员全量 | 通过 |
| 前端契约与过滤 | 4/4 |
| CRM 全量 | 440/440 |
| 真实候选数量 | 7 |
| 审批状态与占用额 MySQL 对账 | 通过 |
| 客户过滤 | 通过 |
| Server/Web production build | 通过 |
