# CRM 性能与容量基线（20260716T233454Z）

- 结论：`PASS`
- 工作负载：5 个只读场景，每场景预热 3 次、采样 100 次、并发 8
- 阈值：错误率 ≤ 1.0%，p95 ≤ 800 ms，p99 ≤ 1500 ms，吞吐 ≥ 5.0 req/s
- 主机：`Linux 7.1.3-2-cachyos x86_64 GNU/Linux`，CPU：20 核，内存：38.8 GiB
- 数据库规模：369 张表，其中 CRM 67 张

## 结果

| 场景 | 请求 | 错误 | 错误率 | p50 | p95 | p99 | 吞吐 | 结论 |
|---|---:|---:|---:|---:|---:|---:|---:|---|
| customer-page | 100 | 0 | 0.00% | 61 ms | 101 ms | 122 ms | 93.21 req/s | PASS |
| business-page | 100 | 0 | 0.00% | 47 ms | 72 ms | 88 ms | 109.06 req/s | PASS |
| customer-summary | 100 | 0 | 0.00% | 48 ms | 92 ms | 98 ms | 106.41 req/s | PASS |
| contract-rank | 100 | 0 | 0.00% | 35 ms | 49 ms | 54 ms | 144.38 req/s | PASS |
| sales-funnel | 100 | 0 | 0.00% | 34 ms | 50 ms | 63 ms | 149.52 req/s | PASS |

## 容器采样

| 容器 | CPU | 内存 |
|---|---:|---:|
| mitedtsm-rootless-server | 8.084904789169503 | 1.754GB / 41.65GB |
| mitedtsm-rootless-mysql | 0.7114845199886302 | 473.1MB / 41.65GB |
| mitedtsm-rootless-redis | 0.09079495830593774 | 6.701MB / 41.65GB |

> 该结果是本地单节点验收基线，不等同于生产容量承诺。原始汇总见 `crm-performance-20260716T233454Z.tsv`。
