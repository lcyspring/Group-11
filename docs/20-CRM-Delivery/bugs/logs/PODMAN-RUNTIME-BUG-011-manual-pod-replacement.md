# PODMAN-RUNTIME-BUG-011：完整部署重复手工删除同名 Pod

日期：2026-07-18。分支：`develop`。级别：P2。状态：已关闭。

## 现象与根因

容器启动早已使用 `podman run --replace`，但完整替换仍在优雅停止后手工执行 `pod rm`，再创建同名
Pod。失败分支又维护一套 `pod rm --force`，使替换路径冗长且容易在 Pod 名存在时产生竞态。

## 修复与边界

- Server 仍先按 YAML `stop_timeout_seconds` 优雅停止，确保 shutdown hook 可访问数据库和消息服务；
- Pod 尝试按同一超时停止，随后统一由 `podman pod create --replace` 原子接管同名替换；
- Server/Web/Mall/基础设施容器继续统一使用 `podman run --replace`；
- `stop.sh` 是显式停服/销毁操作，仍保留 `pod rm`，不错误套用 replace；一次性 Init 容器完成后仍显式清理。

门禁检查部署脚本不存在手工 `pod rm`。真实完整替换已验证旧 Pod 优雅停止、同名 Pod 原子接管、
MySQL 持久卷保留、兼容迁移、9 个 BPM 模型幂等恢复，以及 Server/Web/Mall 三个服务健康检查。
