# 测试结果

日期：2026-07-18

## Ubuntu 26.04 CRM

```bash
cd podman
bash ./compile.sh ./config/test-crm-ubuntu-26.04.kdl
```

- `CrmStatisticsFunnelServiceImplTest`：6/6；
- `CrmStatisticsFunnelMapperTest`：3/3；
- CRM 全量：533/533；
- Failures 0、Errors 0、Skipped 0；
- Maven reactor：20/20 SUCCESS。

首次编译发现 Wrapper 扩展方法调用顺序导致类型退化，修复后完整重跑通过，详见
`TEST-BUG-005`。

## Ubuntu 26.04 Web

```bash
cd podman
bash ./compile.sh ./config/build-web-ubuntu-26.04.kdl
```

- Node 22.22.1、pnpm 11.3.0；
- Vite production build 成功；
- Web 类型检查通过；
- 漏斗/预测前端契约与合同/生日专项合计 8/8；
- 生产构建成功并替换 8081 Web 容器。

## 真实运行验收

- `crm_business.end_time` 已通过已有库兼容迁移并完成历史终态回填；
- 预测 API 返回 `forecastBusinessCount`、`actualBusinessCount`、`forecastAmount`、`actualAmount`；
- 真实请求 `code=0`，无数据周期四项指标稳定返回 0；
- Server 与 Web 运行镜像已分别封装并用 `replace-server`、`replace-web` 替换。
