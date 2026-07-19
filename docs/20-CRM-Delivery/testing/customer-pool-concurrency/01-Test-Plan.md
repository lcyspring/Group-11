# CRM 公海并发领取测试计划

执行日期：2026-07-14

## 目标

验证公海领取在单客户并发、相反顺序批量并发和普通业务回归下满足：

1. 只有一个并发请求成功；
2. 客户只有一个最终负责人；
3. 每位客户只有一条有效 Owner 权限；
4. 每位客户只有一条本轮 `TAKE_POOL` 历史；
5. 相反 ID 顺序的批量请求不死锁；
6. CRM 既有测试、生产 JAR 和 Podman 运行环境不回归。

## 自动化用例

| 层级 | 用例 | 预期 |
|---|---|---|
| Mapper | `selectByIdsForUpdateUsesDeterministicLockOrder` | SQL 包含 `IN`、`ORDER BY id ASC`、`FOR UPDATE` |
| Service | `receiveCustomerRejectsCustomerClaimedBeforeLockAcquisition` | 锁后读到负责人时返回 `1020006001`，不继续写权限和历史 |
| CRM 全量 | `Crm*Test` | 全部通过 |

## 真实 API/MySQL 场景

| 场景 | 并发请求 | 数据断言 |
|---|---|---|
| 单客户 | 用户 1、用户 100 同时领取同一客户 | 一个 code 0，一个 `1020006001`；一个有效 Owner；一条领取历史 |
| 交叉批量 | A 请求 `[id1,id2]`，B 请求 `[id2,id1]` | 无死锁；一个批次成功，一个失败；两位客户状态一致 |
| 清理 | 删除临时客户、权限和历史 | 有效测试客户、有效测试权限为 0 |

真实验证使用 rootless Podman 中的 Server 与 MySQL，不使用项目原 Docker 编排。
