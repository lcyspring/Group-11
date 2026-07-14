# PODMAN-RUNTIME-BUG-003 MySQL 自动更新时间回退 8 小时

## 现象

工单运行验收中，Java 写入的 `create_time` 为 Asia/Shanghai 时间，而 MySQL
`ON UPDATE CURRENT_TIMESTAMP` 使用 UTC，导致状态更新后 `update_time`
比 `create_time` 早 8 小时。

## 根因

rootless MySQL 容器未显式配置时区，Java 运行镜像已使用 Asia/Shanghai，
两端时间基准不一致。

## 修复

- `runtime-local-check.yaml` 和本机 `runtime-local.yaml` 新增
  `mysql.timezone: Asia/Shanghai`；
- `up.sh` 只从 YAML 读取该值，并以容器 `TZ` 传入 MySQL；
- 没有在脚本逻辑中硬编码具体时区。

## 验证

- 容器：`2026-07-14 15:52:30 CST`；
- MySQL：`@@system_time_zone=CST`、`NOW()=2026-07-14 15:52:30`；
- `W-202607-0002` 创建时间 15:52:47，状态更新时间 15:52:58，
  `update_time >= create_time` 为 1；
- YAML 配置测试通过且 Pod 状态在 check 测试前后不变。
