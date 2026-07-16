# 测试计划

## 自动化

- `CrmWorkOrderGovernancePropertiesTest`：YAML 时区、工作时间和围栏边界。
- `CrmWorkOrderGovernanceRequestValidationTest`：坐标、日期和必填字段。
- `CrmWorkOrderSlaCalculatorTest`：工作分钟、周末跳过和节假日覆盖。
- 既有 `CrmWorkOrderServiceImplTest`：创建、开始、完结回归。
- Web `governance.test.mjs`：签到按钮条件和 SLA 状态优先级。

## 运行验收

真实环境需验证：创建带坐标工单、围栏内/外签到、暂停恢复后截止时间平移、节假日跳过、调度升级、无权限和跨租户负向访问。
