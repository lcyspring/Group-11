# 回款逾期检测与提醒测试

## 自动化门禁

```bash
bash ./podman/compile.sh ./podman/config/test-crm-ubuntu-26.04.yaml
```

Ubuntu 26.04 CRM 门禁结果：497 个测试通过，0 失败；新增服务、策略和调度器专项测试覆盖发送成功、
失败重试、配置校验、租约占用和中断恢复。JaCoCo 报告位于
`Server/mitedtsm-module-crm/target/site/jacoco/`。

## 验收场景

1. 准备一个已审批合同下、计划回款日期早于当前日期且未审批通过回款的计划；
2. 执行 `crm-receivable-overdue` 调度任务；
3. 确认提醒事实唯一创建、负责人收到站内信；
4. 重复执行任务，确认同一业务日期不重复发送；
5. 模拟通知失败，确认状态变为失败并在上限内重试；
6. 审批回款后再次执行，确认不再生成新的提醒。
