# 测试结果

日期：2026-07-14

## Ubuntu 26.04 CRM

```bash
cd podman
bash ./compile.sh ./config/test-crm-ubuntu-26.04.yaml
```

- CRM：102/102；
- Failures：0；Errors：0；Skipped：0；
- Maven reactor：20/20 SUCCESS；
- 目标响应字符串契约测试通过。

## Ubuntu 26.04 Web

- Node：22.22.1；pnpm：11.3.0；
- CRM 统计纯函数：7/7；
- 大额精度样例 `9007199254740991.99 + 0.01` 结果为
  `9007199254740992.00`；
- Prettier：通过；
- ESLint：通过；
- Vite production build：成功；
- 未使用宿主机 JDK、Node 或 pnpm。

## Bug 记录

本批测试没有发现新的真实缺陷，不新增 Bug 编号。前端测试启动问题属于已关闭的
`TEST-BUG-007`，日志位于
`docs/10-Testing/bugs/logs/TEST-BUG-007-ubuntu-node-typescript-strip-disabled.md`。
