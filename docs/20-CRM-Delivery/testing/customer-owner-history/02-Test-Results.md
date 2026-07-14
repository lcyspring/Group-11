# CRM 客户归属记录测试结果

执行日期：2026-07-13

## 自动化与构建

| 检查 | 结果 |
|---|---|
| 客户 Service 定向 | 15/15 |
| 客户 Controller 定向 | 3/3 |
| CRM `Crm*Test` 全量 | 54/54，失败 0，错误 0 |
| 相关 Vue/TS ESLint | 通过 |
| Web `build:prod` | 通过 |
| Server 生产 JAR | 通过 |
| Rootless Podman | Server/Web/Mall 健康 |

## 真实 API/MySQL

| 场景 | 实际结果 | 状态 |
|---|---|---|
| 幂等迁移 | 连续执行两次；`previous_owner_user_id`/`new_owner_user_id` 各一列 | 通过 |
| 初始分配 | `type=3, null -> 1, operator=1` | 通过 |
| 转移 | `type=4, 1 -> 100, operator=1` | 通过 |
| 进入公海 | `type=1, 100 -> null, operator=1` | 通过 |
| 领取 | `type=2, null -> 1, operator=1` | 通过 |
| 排序 | API 依次返回 2、1、4、3，与 ID 倒序一致 | 通过 |
| 强制历史写失败 | API `500`；客户 owner 仍为 1，Owner 权限仍属于 1，历史数仍为 1 | 通过 |
| 无数据权限 | `1020007001` | 通过 |
| 跨租户 | `403`，“您无权访问该租户的数据” | 通过 |
| 原公海统计 | 按日期 code 0/31 行；按用户 code 0/11 行 | 通过 |
| 清理 | 有效测试客户、测试历史、临时授权、临时触发器数量均为 0 | 通过 |

## 补充说明

强制回滚测试仅在本地 Podman MySQL 中临时创建触发器，验证后已删除，不属于产品迁移。测试历史已物理清理，不会污染公海统计。
