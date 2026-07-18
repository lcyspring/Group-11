# CRM-FEATURE-063：营销 Provider YAML 显式 provision

## 目标

把 CRM 真实短信/邮件发送所需的系统渠道、账号和模板纳入 `deploy.sh` 使用的 YAML 契约，同时保留
生产环境已有配置的所有权边界。命令行仍只传配置文件路径，示例配置只保存非秘密占位值。

## 模式

- `disabled`：不修改任何 Provider 数据，且短信、邮件开关必须均为 `false`；
- `create-only`：按稳定编码或邮箱补齐不存在的渠道、账号和模板，不覆盖已有配置；
- `managed`：由 YAML 幂等创建或更新稳定键对应的渠道、账号和模板。

启用 provision 时必须设置 `crm_marketing.provider_mode: system`，并至少开启短信或邮件之一。短信
API Key/Secret、供应商模板 ID以及邮件用户名/密码若仍为占位值，部署预检直接拒绝。

## 管理聚合

- 短信：渠道编码、签名、API 凭据、回调地址、模板编码/名称/内容/参数/供应商模板 ID；
- 邮件：邮箱、SMTP 用户/密码/主机/端口、SSL/STARTTLS、模板编码/名称/昵称/标题/HTML/参数；
- 模板始终关联到本次稳定键定位的渠道或账号，不使用不确定的“第一条记录”。

## 安全与部署边界

- `podman/internal/provision-marketing-provider.sh` 只在 `replace` 或 `replace-server` 阶段运行；
- 秘密经过 Base64 后由 stdin 发送给 MySQL，不作为 SQL 参数或日志文本输出；
- 示例 YAML 保留 `not-configured`，真实秘密只进入被 Git 忽略的本机配置；
- `managed` 只管理明确稳定键，不批量清理其他供应商账号；
- 本功能验证数据库配置闭环，不伪造供应商审核、真实发送或回执结果。

## 当前结论

已完成 YAML、部署入口和数据库幂等 provision。`create-only` 重复执行、`managed` 更新、`disabled`
保留和模板外键关联四个隔离场景均通过。真实供应商验收仍取决于账号、模板审核和外部回执协议。
