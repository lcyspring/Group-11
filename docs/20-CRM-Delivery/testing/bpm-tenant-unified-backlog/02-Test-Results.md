# BPM 租户治理与统一待办测试结果

更新日期：2026-07-14。

| 验证项 | 结果 |
|---|---|
| Ubuntu 26.04 BPM 自动化 | 54 个；48 通过、6 跳过、0 失败、0 错误 |
| 新增租户专项 | 任务 3、流程定义/部署 2、模型/BPMN 4，全部通过 |
| 原有用户组分页回归 | 6/6 通过 |
| CRM BPM 专项 ESLint | 通过，警告 0 |
| Ubuntu 26.04 Server 构建 | 通过 |
| Ubuntu 26.04 Web 生产构建 | 通过 |
| 租户 1 待办 API | `code=0`，当前样本总数 0 |
| 租户 1 已办 API | `code=0`，当前样本总数 0 |
| 更新后服务 | Server `UP`，Web 200 |

Server 与 Web 已使用 rootless Podman 热替换；本机运行 KDL 已恢复 `startup_mode: replace`。
