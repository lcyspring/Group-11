# 测试结果

日期：2026-07-18

## Ubuntu 26.04 后端

命令：

```bash
cd podman
bash ./compile.sh ./config/test-crm-ubuntu-26.04.kdl
```

- CRM：533/533；
- Failures：0；
- Errors：0；
- Skipped：0；
- Mapper 契约覆盖配置阶段和 1/2/3 三种终态；
- Service 覆盖阶段累计算法及结单结果占比；
- Maven reactor：20/20 SUCCESS。

## Ubuntu 26.04 前端

命令：

```bash
cd podman
bash ./compile.sh ./config/build-web-ubuntu-26.04.kdl
```

- Node：22.22.1；
- pnpm：11.3.0；
- Vite production build：成功；
- 漏斗/预测前端专项与合同/生日专项合计：8/8；
- 宿主未使用 Node、pnpm 或 JDK。

## 结果

阶段最后存量 `10/4/1` 被计算为累计 `15/5/1`；赢单/输单/无效样本 `2/3/1` 独立返回，
结单占比为 `33.33/50.00/16.67`。真实 API 返回阶段及三个终态，`code=0`。
