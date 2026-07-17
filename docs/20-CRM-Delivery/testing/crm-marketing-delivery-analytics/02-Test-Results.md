# CRM 群发送达与打开分析测试结果

日期：2026-07-17。分支：`develop`。

## Ubuntu 26.04 自动化

- CRM：481/481，失败 0、错误 0、跳过 0；
- System `MailSendServiceImplTest`：10 个，9 通过、1 个原有人工演示测试跳过；
- 群发 Web：10/10，ESLint 零告警；
- Web production build：通过；
- 匿名像素控制器的租户旁路契约已纳入 CRM 测试。

## 真实 Podman 运行验收

- 新迁移连续执行三次无错误；
- `crm_marketing_broadcast_recipient` 的 4 个字段和 2 个索引均准确存在；
- 无效令牌返回 HTTP 200、`Content-Type: image/gif`、`Cache-Control: no-store` 和 34 字节透明 GIF；
- 创建人只能访问自己的任务、收件人、汇总和刷新接口；跨用户访问被拒绝；审核人允许访问；
- 对象范围脚本输出 `marketing-object-scope=ok`、`analytics=allow+deny`，临时数据清理为 0；
- Server、Web 和 Mall 保持运行。

首次运行验收发现匿名像素虽有 `PermitAll`，仍被租户头门禁拦截并返回 JSON。补充
`TenantIgnore` 后重新执行完整自动化、部署和 HTTP 验收，最终结果以上述记录为准。
