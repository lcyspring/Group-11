# PODMAN-RUNTIME-BUG-004 Server 与基础设施并停导致超时

## 现象

第二次全量重建 Pod 时，`podman pod stop --time 120` 中的 Server 在 120 秒内
未退出，最终由 Podman 发送 SIGKILL。

## 根因

原脚本同时停止 Server、MySQL 和 RabbitMQ。Spring 执行关闭钩子时依赖已经同步
进入停止过程，存在关闭顺序竞争，应用可能无法及时完成资源释放。

## 修复

`up.sh` 替换 Pod 和 `down.sh` 正常停机统一调整为：

1. 在数据库和消息代理仍运行时先停止 Server；
2. Server 退出后再停止 Pod 内基础设施和前端；
3. 两阶段都继续使用 YAML 的 `deployment.stop_timeout_seconds`；
4. 不改变数据卷删除策略。

## 验证

脚本语法和 runtime YAML check 测试通过；2026-07-14 全量 Pod 复验中，
Server 先行正常退出，随后 Pod 立即停止并重建，未再等待 120 秒或触发 SIGKILL。
