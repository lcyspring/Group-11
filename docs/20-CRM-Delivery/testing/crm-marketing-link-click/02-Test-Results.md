# CRM 营销链接点击统计测试结果

日期：2026-07-18。环境：Ubuntu 26.04 公开工具链镜像。

| 检查 | 结果 |
|---|---|
| Server 全仓编译 | 通过 |
| CRM 全量自动化 | 532/532；失败 0、错误 0、跳过 0 |
| Web 营销专项 | 13/13；失败 0 |
| Web 专项覆盖率 | 行/分支/函数 100% |
| Web 专项 ESLint | 通过，warning 0 |
| Web production build | 通过 |
| Web 全仓 `vue-tsc --noEmit` | 通过，诊断 0 |
| CRM JaCoCo | 已生成；整体行覆盖率 48.71% |
| 迁移重复执行 | compatibility 部署执行后再连续执行 2 次，均通过 |
| 真实 API/匿名 HTTP 矩阵 | 通过；草稿链接保存/回显/删除、302 固化目标、重复累计、失败/畸形令牌 404、白名单拒绝和汇总口径均符合预期 |
| 三阶段部署健康 | Server `UP`、Web 200、Mall 200 |

执行入口：

```bash
bash podman/compile.sh podman/config/verify-crm-marketing-delivery-ubuntu-26.04.yaml
bash podman/compile.sh podman/config/check-web-types-ubuntu-26.04.yaml
bash podman/tests/acceptance/verify-crm-marketing-link-click.sh podman/config/verify-crm-marketing-link-click.example.yaml
```

CRM 和 Web 构建均使用 `ghcr.io/elel-code/group-11-build-ubuntu:26.04`；Host 未安装或下载项目
JDK、Maven、pnpm 或 `node_modules`。
