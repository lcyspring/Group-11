# BPM-NOTIFY-BUG-001：短信模板缺失导致审批接口报错

## 现象

审批通过后接口提示“短信模板不存在”。当前运行库短信通道和短信模板均为 0 条，但 BPM 无条件发送 `bpm_process_instance_approve`。

## 根因

- 可选的外部短信通知与核心审批事务没有故障隔离；
- 运行 YAML 没有 BPM 短信开关、失败策略和四类模板配置入口；
- 短信详情 URL 依赖应用默认值，没有由部署配置声明用户可访问的管理端地址。

## 修复

- 新增 `mitedtsm.bpm.notification.sms-enabled/fail-fast` 显式属性；默认关闭短信、默认不让通知异常回滚审批；
- 新增四类短信模板 YAML：审批通过、审批拒绝、待办分配、任务超时；
- 部署阶段支持 `disabled/create-only/managed`，启用短信时强制校验唯一通道和四个启用模板；
- `network.admin_ui_public_url` 注入 `mitedtsm.web.admin-ui.url`，短信详情链接不再使用代码固定 IP；
- Provider 失败保留模板 code、用户和根因告警，核心审批结果保持可靠。

## 验证

BPM 单元测试覆盖短信关闭、正常发送、模板缺失隔离、显式 fail-fast 及四类稳定模板契约；运行 YAML 无状态预检通过。
