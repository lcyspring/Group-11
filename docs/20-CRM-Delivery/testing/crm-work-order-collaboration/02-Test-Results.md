# 测试结果

日期：2026-07-16。分支：`develop`。构建环境：Ubuntu 26.04 rootless Podman。

| 检查 | 结果 |
|---|---|
| CRM 自动化回归 | 387/387，通过，失败 0、错误 0、跳过 0 |
| 工单处理组服务专项 | 3/3，通过 |
| 工单服务专项 | 20/20，通过 |
| 工单 Mapper 范围专项 | 5/5，通过 |
| Web 工单纯函数 | 3/3，通过 |
| Web Node 覆盖率 | 行 98.61%、分支 95.00%、函数 100.00% |
| Web 专项 ESLint | 零警告 |
| Web production build | 通过，生成 `Web/dist-prod/index.html` |
| MySQL 协作迁移 | 真实库重复执行通过；处理组、成员、抄送表和工单扩展字段存在 |
| 真实 API 验收 | 通过：处理组创建/更新、最小负载自动派单、抄送视图、未分配池、原子领取、改派/开始/完结；清理残留 0 |

执行入口：

```bash
cd podman
bash ./compile.sh ./config/test-crm-work-order-collaboration-ubuntu-26.04.kdl
bash ./tests/acceptance/verify-crm-work-order-collaboration.sh ./config/verify-crm-work-order-collaboration-local.kdl
```

真实服务保持运行：Web `8081`、Mall `8082`、Server `8080`。验收脚本使用 YAML 中的账号、租户、
客户和用户编号，不在命令行传递业务参数。
