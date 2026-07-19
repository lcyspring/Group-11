# CRM 合规群发管理闭环

本功能将短信/邮件群发从验证页完善为可维护闭环：客户或联系人名单、渠道模板、JSON 参数、
计划时间、草稿编辑删除、BPM 独立审核、发送前置检查、发送、失败重试和逐收件人结果均有明确入口。

群发采用草稿、待审核、驳回、待发送、发送中、已发送、部分失败和取消八态模型。创建者不能
审核自己的任务；驳回原因必填；草稿维护和状态流转均由服务端权限与条件更新保护。待审核行只能进入 BPM 审批详情，页面不再提供绕过流程的直接通过/驳回按钮。

发送前由 `/broadcast/send-readiness` 检查审批状态、模板、SMTP 账号、模板参数和有效收件人。`provider_mode=system` 才会调用真实邮件服务商；`record-only` 仅保留结果快照并在界面显示警告。

客户和联系人使用群发专用候选接口，按 CRM 负责人读取范围过滤，不要求营销操作员额外拥有
联系人管理菜单权限；可选营销活动加载失败也不会拖垮群发列表。

计划发送由 `CrmMarketingBroadcastScheduler` 每分钟扫描当前租户到期任务，使用
`crm:marketing:broadcast` 独立锁。提供商模式、频控、批大小、调度 cron、时区和锁均来自显式
显式 KDL 配置。

## 成功发送邮件

1. 设置 `marketing_provider.provider_mode "system"`，并启用邮件服务商。
2. 配置 SMTP 邮箱、host、port、username 和密码，且模板关联该账号。
3. 启用邮件模板并填写群发模板 code；模板参数必须与 JSON 参数一致。
4. 客户或联系人必须有邮箱，存在邮件渠道同意记录且未退订。
5. 用 `bpm-provision-marketing-outreach.example.kdl` 创建并发布 `crm-marketing-outreach-audit` 模型。
6. 保存草稿、提交审核，在审批中心完成审批；回到列表确认 readiness 无问题后发送。
7. 发送后刷新提供商结果；邮件“已接受”只代表 SMTP 服务商接受，不代表已送达。
