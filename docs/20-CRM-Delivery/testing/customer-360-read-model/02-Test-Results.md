# 客户 360 只读聚合测试结果

更新日期：2026-07-18。

| 验证项 | 结果 |
|---|---|
| CRM 自动化 | 208/208 通过，失败 0、错误 0、跳过 0 |
| Ubuntu 26.04 Server 构建 | 通过 |
| Ubuntu 26.04 Web 生产构建 | 通过 |
| 客户 360 专项 ESLint | 通过，警告 0 |
| MySQL 8 聚合 SQL | 通过；客户 17 返回合同 3、发票 3、合同附件 1 |
| 金额样例 | 合同额 16016.00、已确认回款 16016.000000、有效净开票 0.000000 |
| 真实摘要 API | `code=0`；客户摘要返回真实 `taskCount`，不再返回陈旧 `taskSupported` 占位字段 |
| 发票客户过滤 API | `total=3`，响应 `customerId` 唯一值为 17 |
| 更新后运行服务 | Server `UP`，Web/Mall 均 `200` |

最新 Server/Web 已热替换；本机运行 KDL 使用 `startup_mode: replace-server`，数据库
`dataset_mode: preserve`，现有数据保持不变。
