# STAT-BUG-001：销售漏斗 ref 被乱码注释吞并

发现日期：2026-07-13。关闭日期：2026-07-16。级别：P0。

乱码曾使多个 Tab ref 声明落入注释，运行时点击对应页签会访问未声明变量。修复后四个 ref 均为
独立 UTF-8 可执行声明，并在 `handleQuery` 前初始化。

最终增加 Node SFC 契约测试：以严格 UTF-8 解码文件，逐个验证 `funnelRef`、`businessSummaryRef`、
`businessInversionRateSummaryRef` 和 `salesForecastRef` 的声明及顺序。Ubuntu 26.04 统计前端 10/10、
ESLint 零警告、生产构建通过。
