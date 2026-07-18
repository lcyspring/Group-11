# 支付跨域集成规范覆盖率

| 计划门禁 | 证据 |
|---|---|
| 流水边界和映射键 | `01-Boundary-and-Flows.md`、`03-Data-Dictionary.md` |
| 回调签名、幂等、重放和补偿 | `02-API-and-Errors.md`、`04-Security-and-Compensation.md` |
| OA/CRM/营销触发与禁止依赖 | `01-Boundary-and-Flows.md` |
| API、错误码、状态和审计 | `02-API-and-Errors.md`、`03-Data-Dictionary.md` |
| 权限矩阵、跨租户隔离 | `04-Security-and-Compensation.md` |
| YAML 和评审清单 | `05-YAML-and-Review.md` |

本次 Java 增量是支付回调日志脱敏，专项测试 2/2，Pay 模块全量 167 个中 132 通过、35 个外部集成
测试跳过。规范本身不增加业务代码覆盖率；未来实现不得用文档
覆盖映射替代单元、集成、沙箱 Provider 和运行安全测试。
