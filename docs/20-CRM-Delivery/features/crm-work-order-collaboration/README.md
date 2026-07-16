# CRM-FEATURE-046：客服工单处理组与协作派单闭环

状态：已实现。分支：`develop`。日期：2026-07-16。

## 业务闭环

- 处理组作为独立聚合维护稳定编码、负责人、成员、支持工单类型、启停状态和排序。
- 新工单可选择处理组并自动分派；算法按启用且支持当前类型的组过滤，再按开放工单负载、
  处理组排序、组 ID、用户 ID 稳定决胜。
- 无自动候选人时按 YAML 进入处理组未分配池；处理组成员可通过原子条件更新领取，避免并发双领。
- 支持手工分派和改派。处理组负责人只能管理本组，跨组操作必须具有
  `crm:work-order:assign-all`，权限判断位于服务端。
- 工单支持抄送人去重、人数上限、通知和“抄送我的”视图；抄送只提供读取权，不提供处理权。
- 处理组负责人可读取组内工单；普通成员只可读取本组未分配工单或自己处理的工单。
- 列表按优先级、创建时间和 ID 倒序；新增“处理组未分配”视图、领取动作和完整派单轨迹。
- 管理端增加处理组维护页、候选人开放工单负载、自动分派提示、抄送选择和三语言文案。

## 显式策略

`mitedtsm.crm.work-order-dispatch` YAML 配置声明功能启停、创建时自动派单、无候选回退模式、
最大抄送人数、工单描述最小长度和解决方案最小长度。命令行入口仍只接受 YAML 路径。

## 数据库对象

- `crm_work_order_group`
- `crm_work_order_group_member`
- `crm_work_order_cc`
- `crm_work_order.group_id / dispatch_mode / assign_time`
- 处理组菜单、权限、三语言菜单文案和 `crm-work-order-copied` 通知模板

迁移 `database/migrations/new-crm-work-order-collaboration.sql` 已纳入兼容迁移清单，并在真实 MySQL
重复执行验证。
