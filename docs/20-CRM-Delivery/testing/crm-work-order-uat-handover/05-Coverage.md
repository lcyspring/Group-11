# 工单 UAT 覆盖映射

人工 UAT 不增加 JaCoCo 数值。本目录记录需求场景覆盖，并复用已执行的自动化证据。

| 能力 | UAT | 自动化/工程证据 |
|---|---|---|
| 处理组、自动派单、领取、抄送、跨组权限 | 01、04～08 | `testing/crm-work-order-collaboration/` |
| 来源、状态机、退回重提、待办 | 02、03、09、10、14 | `testing/crm-work-order-minimum-closure/` |
| 移动签到、围栏、SLA、工作日历 | 11～13 | `testing/crm-work-order-service-governance/` |
| 客户 360、统计 | 15 | `testing/customer-360-work-orders/`、`testing/crm-work-order-statistics/` |
| 越权、非法输入、并发和清理 | 06、07、08、16 | `testing/crm-work-order-performance-security/` |

工程基线：50 并发业务链 250/250、专项安全负向 7/7，临时数据残留 0。CRM 全量自动化和覆盖率
以 `testing/README.md` 的最近一次容器执行记录为准；人工签署时必须填写实际使用的提交和镜像摘要。

