# CRM 合规群发测试结果

日期：2026-07-16。环境：Ubuntu 26.04 构建容器、rootless Podman 运行服务。

| 检查 | 结果 |
|---|---|
| Server package | 通过 |
| CRM 全量自动化 | 451/451，通过，失败 0、错误 0、跳过 0 |
| 群发服务、校验和调度专项 | 15/15，通过 |
| Web 群发专项 | 8/8，通过 |
| Web ESLint | 零警告 |
| Web production build | 通过 |
| 真实草稿 CRUD | 创建、详情、编辑、删除通过 |
| 权限化目标候选 | 当前用户可读客户及其联系人通过，无需联系人菜单权限 |
| 真实审核闭环 | 提交、驳回、修订、重提、通过全部通过 |
| 真实发送闭环 | record-only 发送、任务汇总、收件人结果通过 |
| 验收数据清理 | 群发任务 0、同意记录 0 条残留 |

运行入口：

```bash
bash ./podman/compile.sh ./podman/config/verify-crm-outreach-ubuntu-26.04.yaml
bash ./podman/compile.sh ./podman/config/verify-crm-outreach-web-ubuntu-26.04.yaml
bash ./podman/verify-crm-outreach.sh ./podman/config/verify-crm-outreach-local.yaml
```
