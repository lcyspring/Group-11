# 测试结果：CRM 营销 Provider provision

执行日期：2026-07-18。

命令：

```bash
bash podman/tests/marketing-provider-provision/run.sh podman/config/runtime-local.yaml
```

| 用例 | 结果 |
|---|---|
| `create-only` 重复执行 | 通过，短信渠道/模板、邮件账号/模板均无重复 |
| `managed` 稳定键更新 | 通过，四个聚合字段均更新为 v2 |
| `disabled` 保留现状 | 通过，v2 值未被 v3 覆盖 |
| 模板关联完整性 | 通过，两个模板均指向预期渠道或账号 |

汇总：4/4 通过，失败 0。隔离测试库已自动删除。真实部署使用 `disabled`，现有 Provider 配置未改变。
