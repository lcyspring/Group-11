# CRM 性能基线运行证据

日期：2026-07-17。分支：`develop`。

执行入口：

```bash
bash ./podman/tests/acceptance/verify-crm-performance-baseline.sh \
  ./podman/config/verify-crm-performance-baseline-local.yaml
```

`runs/` 保存按 UTC 运行编号生成的 Markdown 报告和 TSV 原始汇总。共享模板不包含真实密码，
本机 `*-local.yaml` 被 Git 忽略。

当前基线：5 个场景、500/500 成功、错误率 0%，p95 最大 101 ms，p99 最大 122 ms，
最低吞吐 93.21 req/s，结论为 PASS。
