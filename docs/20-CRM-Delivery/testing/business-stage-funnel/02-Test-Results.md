# 测试结果

日期：2026-07-14

## Ubuntu 26.04 后端

命令：

```bash
cd podman
bash ./compile.sh ./config/test-crm-ubuntu-26.04.yaml
```

- CRM：88/88；
- Failures：0；
- Errors：0；
- Skipped：0；
- 新增：请求校验 1、Mapper 契约 1、Service 累计算法 1；
- Maven reactor：20/20 SUCCESS。

## Ubuntu 26.04 前端

命令：

```bash
cd podman
bash ./compile.sh ./config/build-web-ubuntu-26.04.yaml
```

- Node：22.22.1；
- pnpm：11.3.0；
- Vite production build：成功；
- 宿主未使用 Node、pnpm 或 JDK。

## 结果

测试样本 `10/4/1/2` 被计算为累计 `17/7/3/2`，相邻转化率为
`100.00/41.18/42.86/66.67`，金额累计同样通过断言。
