# 测试计划：部署期数据库 provision

## 环境

- rootless Podman；
- 运行 KDL 的 `image.mysql_base` 官方 MySQL 镜像；
- 每次运行创建独立临时容器、named volume 和测试数据库，结束后自动清理；
- 真实 bootstrap、compatibility 与 `none` dataset manifest。

## 用例

1. `initialize-empty` 对确认空库执行 bootstrap、数据集和 compatibility；
2. 再次执行时识别完整已有库，保留数据并仅重放幂等 compatibility；
3. 非空但没有 `system_users` 标记的未知库拒绝 bootstrap；
4. `require-existing` 对空库返回失败。

## 通过标准

- 空库生成超过 300 张表且 `system_users=1`；
- `none` 数据集执行后 `crm_customer=0`；
- 重复执行不重复插入管理员；
- 两条拒绝路径返回非零退出码；
- 临时容器、卷和测试目录无残留。
