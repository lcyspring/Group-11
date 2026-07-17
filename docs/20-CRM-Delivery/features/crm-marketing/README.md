# CRM 营销互动闭环

分支：`develop`
决策：`ADR-016-marketing-outreach-compliance.md`

## 已交付

- 营销活动：草稿、启动、锁定、终止、完成；预算/目标/负责人和线索、客户、商机、任务关联。
- 竞争对手资料：负责人范围内分页和负责人/状态筛选，真实用户选择，新增、编辑、删除，以及优势、劣势、应对策略和备注的完整维护；网站仅接受 HTTP(S)。
- 合规群发：短信、邮件、双渠道；客户/联系人名单快照；无效联系方式排除；同意/退订检查；审核、发送、失败重试和结果分页；短信送达回执、邮件服务商接受与首次打开采用不同指标口径。
- 频控：批次大小、月度收件人数和提供商模式均来自 YAML；默认 `record-only`，配置 `system` 后复用系统短信/邮件 API。
- 客户关怀：生日/节假日规则、启停、事件日期幂等记录和自动调度；发送前再次执行同意/退订检查。
- 所有业务表带租户、逻辑删除和审计字段；收件人快照不可因客户资料后续变化而隐式改变。

## API 分组

- `/crm/marketing/campaign/*`
- `/crm/marketing/competitor/*`
- `/crm/marketing/outreach/*`
- `/crm/marketing/care/*`

## 配置

`Server/mitedtsm-server/src/main/resources/application.yaml` 的 `mitedtsm.crm.marketing` 是唯一业务阈值来源。命令行仅指定构建/运行 YAML 路径。
