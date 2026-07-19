# CRM 客户名称长度校验测试结果

执行日期：2026-07-14

| 检查 | 结果 |
|---|---|
| `CrmCustomerSaveReqVOTest` | 2/2 |
| 100 字符名称 | 0 个约束错误，通过 |
| 101 字符名称 | 命中指定 `@Size` 消息，通过 |
| Podman 真实 API | code 400，“请求参数不正确:客户名称长度不能超过 100 个字符” |
| 数据库副作用 | 有效的 101 字符客户数量为 0 |
| CRM `Crm*Test` 全量 | 58/58，失败 0，错误 0 |
| 相关 Vue/TS ESLint | 通过 |
| Web `build:prod` | 通过 |
| Server 生产 JAR / Podman | 构建成功，Server/Web/Mall 健康 |

结论：`TC-CUS-003` 的名称长度边界已由前后端同时落实。
