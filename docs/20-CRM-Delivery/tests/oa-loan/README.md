# OA 借款结构化测试记录

| 层级 | 门禁 | 结果 | 覆盖率说明 |
|---|---|---|---|
| 后端领域服务 | `BpmOALoanServiceImplTest` | 4/4 通过 | 覆盖额度升级、未还阻断、审批余额和还款约束核心分支 |
| BPM 模块回归 | `build.bpm_tests: true` | 67 项，0 失败，6 项条件跳过 | JaCoCo 报告位于 BPM 模块 `target/site/jacoco/` |
| Web 业务契约 | `verify:oa-loan` | 3/3 通过 | 行/分支/函数 100%，范围仅为借款契约测试文件 |
| Web production | Vite `build --mode prod` | 通过 | 验证类型转换、打包和动态路由依赖 |

执行环境为 `ghcr.io/elel-code/group-11-build-ubuntu:26.04`，普通构建不重建工具链镜像。
