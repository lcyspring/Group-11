# 数据集集中模式测试结果

- 日期：2026-07-18
- 环境：rootless Podman、MySQL 8.0.46
- 状态：通过

| 检查项 | 结果 |
|---|---|
| runtime KDL 全量配置回归 | 通过，Pod 状态未改变 |
| insert 携带 cleanup | 预检拒绝 |
| replace 缺少首项 cleanup | 预检拒绝 |
| 空库 bootstrap + dataset | 通过 |
| 已有完整库 preserve | 数据保留，仅执行兼容迁移 |
| 未识别非空库 / require-existing 空库 | 均拒绝 |
| 50 条/域 replace | cleanup、核心插入、关联插入顺序成功 |
| replace 后默认配置 | 已恢复 `preserve` |
