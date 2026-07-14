# 测试结果

日期：2026-07-14

## Ubuntu 26.04 CRM

```bash
cd podman
bash ./build-in-ubuntu.sh ./config/test-crm-ubuntu-26.04.yaml
```

- `CrmStatisticsFunnelServiceImplTest`：3/3；
- `CrmStatisticsFunnelMapperTest`：2/2；
- CRM 全量：80/80；
- Failures 0、Errors 0、Skipped 0；
- Maven reactor：20/20 SUCCESS。

首次编译发现 Wrapper 扩展方法调用顺序导致类型退化，修复后完整重跑通过，详见
`TEST-BUG-005`。

## Ubuntu 26.04 Web

```bash
cd podman
bash ./build-in-ubuntu.sh ./config/build-web-ubuntu-26.04.yaml
```

- Node 22.22.1、pnpm 11.3.0；
- Vite production build 成功；
- `Web/dist-prod/index.html`：3694 bytes。

## 未执行项

未构造真实 MySQL 多阶段、跨部门预测数据进行 API 对账；SQL 契约测试不能替代
`STAT-DATA-001` 和 `STAT-SEC-001` 的运行时证据。
