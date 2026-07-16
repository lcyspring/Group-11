# Mitedtsm 本地构建与运行

旧的 `dev/`、`docker-compose/` 和 `docker-images/` 已移除。项目统一使用 rootless
Podman，不再使用 Docker Compose、Docker Desktop 或仓库内镜像归档。

## 构建

所有构建入口只接受一个 YAML 配置路径，构建环境为 Ubuntu 26.04：

```bash
cd podman
bash ./build-in-ubuntu.sh ./config/build-ubuntu-26.04.yaml
bash ./build-mall-h5-in-ubuntu.sh ./config/build-mall-h5-ubuntu-26.04.yaml
```

`build-assets.sh` 仅供其他 Ubuntu 26.04 主机成员使用；本机使用上面的容器入口。

## 运行

复制一个本地运行 YAML，设置 `operation.startup_mode: full` 或 `fast`，然后执行：

```bash
cd podman
bash ./up.sh ./config/runtime-local.yaml
```

服务入口：管理后台 `http://127.0.0.1:8081/`，商城 `http://127.0.0.1:8082/`，后端健康检查
`http://127.0.0.1:8080/actuator/health`。停止服务：

```bash
bash ./down.sh ./config/runtime-local.yaml
```

数据库、Redis、RabbitMQ、TDengine 和各服务均由 Podman Pod 管理；不要再执行历史 Docker
Compose 命令。项目交付文档位于 `docs/20-CRM-Delivery/`。
