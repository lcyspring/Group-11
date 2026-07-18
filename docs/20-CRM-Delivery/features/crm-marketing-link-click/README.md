# CRM-FEATURE-061：营销链接逐收件人点击统计

状态：已完成并通过真实 HTTP 验收。分支：`develop`。日期：2026-07-18。

## 用户闭环

营销群发草稿可以维护有业务含义的跟踪链接编码、名称和目标地址。短信/邮件发送时，服务端为每个
成功发送的收件人生成专属匿名跳转地址；群发详情同时展示跟踪人数、独立点击人数、累计点击次数、
独立点击率和逐链接明细。重复点击只增加累计次数，独立点击人数只计算一次。

## 安全与数据口径

- 匿名入口只接受 48 字符随机令牌，目标 URL 从数据库固化事实读取，不接受请求参数中的跳转地址；
- 保存草稿时只接受 HTTP/HTTPS，并要求 Host 位于 YAML `crm_marketing.click_allowed_hosts` 白名单；
- 仅状态为发送成功的收件人令牌可以跳转，失败发送、无效和畸形令牌统一返回 404；
- 首次点击时间只写一次，最近点击时间和累计次数使用原子 SQL 更新；
- 总独立点击率 = 至少点击一次的收件人数 / 生成跟踪链接的收件人数；逐链接使用相同口径；
- `record-only` 演示发送不生成点击事实，避免演示记录污染真实送达和点击指标。

## 接口、表与配置

```text
GET /app-api/crm/marketing/click/{token}
GET /admin-api/crm/marketing/outreach/delivery-summary?id={broadcastId}
```

`crm_marketing_link` 保存群发级链接定义，`crm_marketing_link_recipient` 保存逐收件人令牌及点击事实。
幂等迁移为 `database/migrations/new-crm-marketing-link-click.sql`，已登记 bootstrap 与 compatibility
manifest。

运行 YAML 显式配置：

```yaml
crm_marketing:
  click_tracking_enabled: true
  click_allowed_hosts: example.com,localhost,127.0.0.1
  max_links_per_broadcast: 10
```

`public_base_url` 决定写入短信/邮件模板参数的公开跳转前缀；生产环境必须使用外部可访问的 HTTPS
地址，并把允许的业务落地页域名精确列入白名单。

## 验证入口

```bash
bash podman/compile.sh podman/config/verify-crm-marketing-delivery-ubuntu-26.04.yaml
bash podman/compile.sh podman/config/check-web-types-ubuntu-26.04.yaml
bash podman/tests/acceptance/verify-crm-marketing-link-click.sh podman/config/verify-crm-marketing-link-click.example.yaml
```

命令行只传 YAML，项目依赖在 Ubuntu 26.04 容器运行时下载或复用 Podman named volume。
