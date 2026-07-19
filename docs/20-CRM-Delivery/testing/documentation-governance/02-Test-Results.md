# 文档治理测试结果

日期：2026-07-18。分支：`develop`。状态：通过。

| 检查 | 结果 |
|---|---|
| Shell 语法 | `podman/tests/documentation/run.sh` 通过 `bash -n` |
| KDL 入口 | `runtime-local-check.kdl` 由 dasel 成功解析 |
| Markdown 相对链接 | 56 个，失效 0 |
| 本机绝对路径链接 | 0 |
| 旧 Podman YAML/旧启动模式/旧文档树引用 | 0 |
| Docker/Compose 遗留入口 | 0 |
| 状态变更 | 无；脚本不调用 Podman 或数据库 |

历史 Bug 日志与 daily 报告不参与旧术语门禁，以保留问题发生时的真实配置和脚本名称。
