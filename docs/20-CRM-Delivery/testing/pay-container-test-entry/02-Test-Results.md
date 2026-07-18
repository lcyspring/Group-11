# Pay 容器测试入口测试结果

执行日期：2026-07-18。

```bash
cd podman
bash ./compile.sh ./config/test-pay-ubuntu-26.04.kdl
```

| 项目 | 结果 |
|---|---|
| 构建环境 | Ubuntu 26.04 公共工具链镜像 |
| Maven reactor | 19/19 SUCCESS |
| Pay 测试 | 167 个 |
| 通过 | 132 个 |
| 跳过 | 35 个外部 Provider 集成测试 |
| 失败 / 错误 | 0 / 0 |
| JaCoCo | `jacoco.csv`、HTML 报告均已生成 |
| 非法组合 | `pay_coverage=true` 且 `pay_tests=false` 被入口拒绝 |
| 总耗时 | 1 分 27 秒 |

关联模块因 `-am` 参与 reactor，均构建成功；测试统计以 Pay 模块自身 Surefire 汇总为准。
