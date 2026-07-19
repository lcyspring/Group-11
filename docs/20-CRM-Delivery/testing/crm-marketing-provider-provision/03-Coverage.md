# 覆盖率：CRM 营销 Provider provision

本项为 Bash/MySQL 集成 provision，不进入 Java JaCoCo。以下记录模式与数据库聚合的功能覆盖率。

| 覆盖对象 | 覆盖 |
|---|---|
| provision 模式 | `disabled/create-only/managed`，3/3 |
| 受管聚合 | 短信渠道、短信模板、邮件账号、邮件模板，4/4 |
| 幂等性 | create-only 重复执行，1/1 |
| 更新与保留 | managed 更新、disabled 保留，2/2 |
| 外键关联 | 短信与邮件模板关联，2/2 |

未计入：真实短信/邮件发送、供应商模板审核、回执和生产 SMTP/TLS 握手。这些属于外部 Provider UAT，
不能以本地数据库 provision 结果代替。
