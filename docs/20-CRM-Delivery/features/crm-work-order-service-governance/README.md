# CRM 工单移动签到与 SLA 治理

本功能关闭 CRM 工单的移动签到、地理围栏、SLA 工作日历、暂停和自动升级闭环。

## 已交付

- `database/new/new-crm-work-order-service-governance.sql`：签到记录、SLA 策略、工作日历、SLA 实例及权限菜单。
- 工单保存服务地点和签到要求；处理人通过 `/crm/work-order/check-in` 完成服务端距离校验。
- SLA 实例在工单创建事务中按优先级初始化，截止时间跳过周末和数据库工作日历覆盖日。
- `/crm/work-order/sla/pause`、`/resume` 提供处理人暂停/恢复，恢复会平移截止时间。
- Redisson 锁保护的定时调度器自动写入 SLA 升级/逾期轨迹。
- 管理端工单表单支持服务坐标和围栏半径，详情页支持浏览器定位签到及 SLA 操作。

## 关键边界

移动签到不采集后台轨迹，不用客户端上报的距离作为事实；SLA 逾期不替换工单业务状态，只通过 SLA 实例状态和轨迹表达。
