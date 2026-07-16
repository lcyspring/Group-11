# CRM 客户关怀完整闭环

客户关怀现支持三类规则：联系人生日当天、指定节假日、客户成交后第 N 天定期回访。

- 计划支持筛选、详情、创建、编辑、启停和停用后删除；
- 节假日仅面向成交客户首联系人，定期回访以成交生命周期变更时间为事实源；
- 自动任务使用 YAML 时区、Cron、开关和分布式锁；
- 发送前统一检查渠道地址、同意/退订、每日频控和租户月度配额；
- 记录按事件日幂等，保存抑制、记录、成功或失败结果；
- 记录和近期生日查询遵循 CRM 客户负责人读取范围；
- 前端提供计划、祝福记录和客户生日查询三个页签。

运行入口：

```bash
bash ./podman/build-in-ubuntu.sh ./podman/config/verify-crm-customer-care-ubuntu-26.04.yaml
bash ./podman/verify-crm-customer-care.sh ./podman/config/verify-crm-customer-care-local.yaml
```
