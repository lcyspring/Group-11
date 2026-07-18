# 测试结果

日期：2026-07-14

## Ubuntu 26.04 CRM

```bash
cd podman
bash ./compile.sh ./config/test-crm-ubuntu-26.04.yaml
```

- `CrmStatisticsPortraitServiceImplTest`：1/1；
- `CrmStatisticsPortraitMapperTest`：1/1；
- CRM 全量：76/76；
- Failures 0、Errors 0、Skipped 0；
- Maven reactor：20/20 SUCCESS；
- JaCoCo 报告成功分析 113 个生产类。

## Ubuntu 26.04 Web

```bash
cd podman
bash ./compile.sh ./config/build-web-ubuntu-26.04.yaml
```

- Node 22.22.1；
- pnpm 11.3.0；
- Vite production build 成功；
- `Web/dist-prod/index.html`：3694 bytes。

## 未执行项

本轮没有构造真实 MySQL 多租户/多部门客户数据进行 API 对账，因此不把 Mapper
结构测试写成运行时数据权限证明。该项并入 `STAT-DATA-001`、`STAT-SEC-001`。
