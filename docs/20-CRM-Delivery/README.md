# CRM 完善工作独立交付目录

本目录集中保存本轮依据项目文档、两套 Gap Analysis 和原型开展的分析、实现、测试、
ADR、Bug 与 Podman 运行证据，不与项目原有 `docs/develop` 或其他原始文档目录混放。

| 目录 | 内容 |
|---|---|
| `features/` | 每个 CRM 功能闭环的独立实现说明与总索引 |
| `testing/` | 每个功能的测试计划、结果和覆盖率 |
| `bugs/` | Bug 总表及每个 Bug 的独立日志 |
| `decisions/` | 销售、财务和工程 ADR |
| `build/` | Ubuntu 26.04、HBuilderX 等构建记录 |
| `runtime/` | 显式 YAML + Podman 运行记录 |

本目录作为独立交付物纳入 `develop` 分支并随源码上传；本机凭据、临时脚本、构建产物
和 JaCoCo 原始 HTML 报告仍不提交。文档内部引用项目原始 Gap/原型作为输入证据，但
不会修改其历史语义来冒充交付结果。
