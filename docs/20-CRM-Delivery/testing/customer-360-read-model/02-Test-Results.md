# 客户 360 只读聚合测试结果

更新日期：2026-07-14。

| 验证项 | 结果 |
|---|---|
| CRM 自动化 | 208/208 通过，失败 0、错误 0、跳过 0 |
| Ubuntu 26.04 Server 构建 | 通过 |
| Ubuntu 26.04 Web 生产构建 | 通过 |
| 客户 360 专项 ESLint | 通过，警告 0 |
| MySQL 8 聚合 SQL | 通过；客户 17 返回合同 3、发票 3、合同附件 1 |
| 金额样例 | 合同额 16016.00、已确认回款 16016.000000、有效净开票 0.000000 |
| 真实摘要 API | `code=0`；客户 17 的对象计数、金额和 `taskSupported=false` 正确 |
| 发票客户过滤 API | `total=3`，响应 `customerId` 唯一值为 17 |
| 更新后运行服务 | Server `UP`，Web/Mall 均 `200` |

Server 与 Web 已分别使用 `rebuild-server`、`rebuild-web` 热替换；本机 YAML 已恢复
`startup_mode: full`，继续保留完整重建作为下一次默认启动方式。
