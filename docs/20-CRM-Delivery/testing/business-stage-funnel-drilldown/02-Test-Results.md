# 测试结果

日期：2026-07-14

## Ubuntu 26.04 CRM

```bash
cd podman
bash ./build-in-ubuntu.sh ./config/test-crm-ubuntu-26.04.yaml
```

- Tests：91/91；
- Failures：0；
- Errors：0；
- Skipped：0；
- Maven reactor：20/20 SUCCESS；
- 新增请求校验 1 项、普通阶段分页 1 项、赢单分页 1 项。

## Ubuntu 26.04 Web

- Prettier：通过；
- ESLint：通过；
- Vite production build：成功；
- Node：22.22.1；
- pnpm：11.3.0。

## 数据清理

单元测试使用 Mapper 和状态服务代理，没有写入数据库，无测试数据需要清理。
