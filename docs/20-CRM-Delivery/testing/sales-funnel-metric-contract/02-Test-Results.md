# 测试结果

日期：2026-07-14

## Ubuntu 26.04 后端

命令：

```bash
cd podman
bash ./build-in-ubuntu.sh ./config/test-crm-ubuntu-26.04.yaml
```

- CRM：65/65；
- Failures：0；
- Errors：0；
- Skipped：0；
- `CrmStatisticsDateRangeTest` 已实际执行；
- Maven reactor：20/20 SUCCESS。

## Ubuntu 26.04 前端

命令：

```bash
cd podman
bash ./build-in-ubuntu.sh ./config/build-web-ubuntu-26.04.yaml
```

- Node 22.22.1；
- pnpm 11.3.0；
- Vite production build 成功；
- `Web/dist-prod/index.html`：3694 bytes。
