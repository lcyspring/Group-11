# 成交周期数据质量测试结果

- 日期：2026-07-16
- 分支：`develop`
- 环境：Ubuntu 26.04 构建容器 + rootless Podman 运行服务
- 状态：通过

| 检查项 | 结果 |
|---|---|
| Shell 语法 | 通过 |
| CRM 全量自动化 | 429/429，失败 0、错误 0、跳过 0 |
| 统计前端 Node | 11/11 |
| 统计目录 ESLint | 零警告 |
| Server production build | 通过 |
| Web production build | 通过 |
| MySQL 负样本 | 验收用户 1 为 1 条，成交周期 `-16` |
| 日期 API | 负样本数 1:1，仍返回负周期 |
| 员工 API | `negativeSampleCount=1` |
| 地区 API | 负样本合计与 MySQL 一致 |
| 产品 API | 两个真实产品分组均显式暴露该负样本 |
| YAML 指标目录 | `customer.deal-cycle` 已运行返回 |
| 验收数据残留 | 无；本验收未创建业务数据 |

构建命令只传入 `podman/config/verify-crm-statistics-data-quality-ubuntu-26.04.kdl`，运行验收命令只传入本机已忽略的 KDL 配置路径。
