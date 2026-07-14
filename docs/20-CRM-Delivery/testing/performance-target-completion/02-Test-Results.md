# 测试结果

日期：2026-07-14

## Ubuntu 26.04 CRM

- CRM：102/102；
- Failures：0；Errors：0；Skipped：0；
- Maven reactor：20/20 SUCCESS；
- 目标完成度服务新增 3 项，Mapper 结构契约新增 1 项。

## Ubuntu 26.04 Web

- Node：22.22.1；pnpm：11.3.0；
- Prettier：通过；
- ESLint：通过；
- CRM 统计纯函数：4/4；
- Vite production build：成功；
- 全部通过 builder 容器执行，未使用宿主机 JDK、Node 或 pnpm。

## Bug 回归

`TEST-BUG-007` 修复前 0/4 测试可启动；使用项目锁定 TypeScript 的 ESM test loader
后 4/4 通过，无实验 stripping 依赖和警告。
