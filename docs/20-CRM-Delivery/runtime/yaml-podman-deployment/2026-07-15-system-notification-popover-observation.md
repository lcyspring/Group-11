# 2026-07-15 顶部站内信弹层观察环境

## 构建与部署

- 分支：`develop`；
- 构建配置：`podman/config/verify-web-notification-ubuntu-26.04.yaml`；
- 部署配置：`podman/config/runtime-local.yaml`；
- Loader 专项 3/3、通知目录 ESLint 零警告、Web production build 全部通过；
- rootless Podman 保留持久卷重建 Web 运行镜像和整个 Pod；
- 容器内 `index.html` 与 Ubuntu 构建产物 SHA-256 一致。

## 运行证据

| 项目 | 结果 |
|---|---|
| Server 健康 | `UP` |
| Web 首页 | HTTP 200，主机请求约 0.3 ms |
| Mall 首页 | HTTP 200 |
| 未读数量接口 | 返回当前用户未读数量，日志耗时约 12 ms |
| 未读列表接口 | 返回当前用户最近未读消息，日志耗时约 11 ms |

接口耗时证明原问题不是慢 SQL。新 Web 已在 `http://127.0.0.1:8081/` 运行，首次点击会立即显示预取缓存
或加载骨架；打开弹层不会调用标记已读接口，也不会把红点在本地强制清零。

## 当前观察地址

- Server：`http://127.0.0.1:8080/actuator/health`；
- Web：`http://127.0.0.1:8081/`；
- Mall：`http://127.0.0.1:8082/`。
