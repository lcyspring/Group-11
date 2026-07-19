# PODMAN-RUNTIME-BUG-006：Web 早于 Server 就绪导致短暂 502

## 现象

完整 Pod 重建时，Web 首页已可访问，但 Spring Boot 仍在启动；用户此时操作页面会收到
Axios `502 ERR_BAD_RESPONSE`。

## 根因

启动脚本为缩短关键路径，并行等待 Web 和 Server 健康检查。Nginx 静态文件几乎立即
就绪，但 `/admin-api` 上游尚未监听，因此产生一个“页面可见、API 不可用”的窗口。

## 修复

完整启动改为先启动并等待 Server 健康，再启动 Web/Mall。这样服务重建期间不会暴露
一个看似可用但所有业务请求返回 502 的管理端。

`rebuild-server` 是显式维护操作，仍存在 Server 自身重启窗口；脚本会等待健康后才报告
完成，Web 与基础设施不会重建。
