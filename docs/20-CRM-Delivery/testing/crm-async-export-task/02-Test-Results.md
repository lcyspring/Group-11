# CRM 异步导出任务测试结果

日期：2026-07-18。环境：Ubuntu 26.04 构建容器。

| 检查 | 结果 |
|---|---|
| CRM 全量自动化 | 527/527，通过；失败 0、错误 0、跳过 0 |
| Web 导出契约 | 5/5，通过 |
| Web 专项 ESLint | 通过 |
| Web production build | 通过 |
| Web 全仓 `vue-tsc --noEmit` | 通过；脚本与 Vue 模板诊断 0 |
| CRM JaCoCo | 已生成；整体行覆盖率 48.26% |
| 真实 HTTP 安全矩阵 | 通过；非空快照、成功五态、跨用户拒绝、单次令牌、防重放、权限撤回拒绝、过期转态与受保护文件删除均符合预期 |

执行入口：

```bash
bash podman/build-in-ubuntu.sh podman/config/test-crm-ubuntu-26.04.yaml
bash podman/build-in-ubuntu.sh podman/config/verify-crm-customer-export-ubuntu-26.04.yaml
bash podman/build-in-ubuntu.sh podman/config/check-web-types-ubuntu-26.04.yaml
bash podman/verify-crm-customer-export.sh podman/config/verify-crm-customer-export.example.yaml
```

构建镜像为公开的 `ghcr.io/elel-code/group-11-build-ubuntu:26.04`，项目依赖在容器运行时下载或复用
named volume，Host 不拉取 `node_modules`。
