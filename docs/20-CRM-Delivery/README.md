# CRM 当前交付文档

本目录集中保存本轮依据项目文档、两套 Gap Analysis 和原型开展的分析、实现、测试、
ADR、Bug 与 Podman 运行证据，不与项目原有 `docs/develop` 或其他原始文档目录混放。

| 目录 | 内容 |
|---|---|
| `features/` | 每个 CRM 功能闭环的独立实现说明与总索引 |
| `testing/` | 每个功能的测试计划、结果和覆盖率 |
| `bugs/` | Bug 总表及每个 Bug 的独立日志 |
| `decisions/` | 销售、财务和工程 ADR |
| `build/` | Ubuntu 26.04、HBuilderX 等构建记录 |
| `runtime/` | KDL + Podman 当前运行契约与汇总证据 |
| `evidence/` | 真实 API、性能、备份恢复与运行证据 |
| `operations/` | 中文运维、可观测和故障处置手册 |
| `planning/` | 剩余功能包、依赖和推进顺序 |
| `integration/` | CRM/OA/营销与支付等跨域契约、数据和安全边界 |
| `user-guide/` | 按菜单和业务闭环组织的人工验收指南 |

## 快速入口

- [技术栈基线与清理边界](TECH_STACK_ZH.md)
- [文档治理规范](DOCUMENTATION_GOVERNANCE_ZH.md)
- [测试总览与覆盖率](testing/README.md)
- [Bug 总表与独立日志](bugs/README.md)
- [功能总索引](features/README.md)
- [人工验收指南](user-guide/README.md)
- [编译、镜像、部署操作手册](../../podman/OPERATIONS_ZH.md)
- [KDL 全字段参考](../../podman/config/KDL_FIELDS_ZH.md)

## 文档边界

`docs/01-*` 至 `docs/05-*` 和 `docs/Proj-Docs-v-6/` 是输入基线；本目录是 `develop` 分支的当前
交付结果。临时运行观察不在这里逐次堆积，仍有复现价值的结论应进入功能测试、Bug 日志或汇总证据。

本目录作为独立交付物纳入 `develop` 分支并随源码上传；本机凭据、临时脚本、构建产物
和 JaCoCo 原始 HTML 报告仍不提交。文档内部引用项目原始 Gap/原型作为输入证据，但
不会修改其历史语义来冒充交付结果。

当前完成状态见 `planning/crm-completion-audit/`；源 Gap 文档仍保留分析时点语义。文档新增、归档、
链接和敏感信息规则统一遵守 [文档治理规范](DOCUMENTATION_GOVERNANCE_ZH.md)。
