# BPM 发起导航与通知可靠性测试结果

## 自动化结果

| 门禁 | 结果 |
|---|---|
| `verify:bpm-start-navigation` | 8/8 通过，0 失败；行/分支/函数覆盖率 100% |
| BPM 全量单元测试 | 74 个执行，68 通过、6 个既有条件跳过，0 失败、0 错误 |
| `BpmMessageServiceImplTest` | 5/5 通过 |
| 运行 YAML 配置测试 | 通过；Pod 状态未改变 |
| Server 完整构建 | Ubuntu 26.04 通过 |
| Web 生产构建与语言包完整性 | Ubuntu 26.04 通过 |

## 运行数据审计

- `system_sms_channel`：0 条；
- `system_sms_template`：0 条；
- 因此运行配置保持 `bpm.notification_sms_enabled: false`；
- 未来启用真实短信前，部署会要求通道和四类模板完整，不再等到用户审批时才暴露缺项。
- 43 条可明确判定的历史审批详情路径已修复，并写入 `bpm_custom_view_path_repair_record`；
- CRM/OA 受管流程的详情路径均已显式化；未知测试模型 `jiandan:1` 的值 `2` 保持原样，前端会拒绝
  加载，不做猜测性兼容。

## 人工回归

部署最新 Server/Web 后检查：分类按钮有明确筛选效果；流程卡片不进入 404；短信关闭时审批通过不再出现“短信模板不存在”；流程表达式在中文、英文、阿拉伯语下无固定中文或键名泄漏。
