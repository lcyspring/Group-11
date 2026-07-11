# Podman 全流程操作指南

本指南适用于团队成员在 Ubuntu 上构建并运行项目，以及在 Windows、虚拟机宿主机或局域网内访问系统。

## 1. 先明确谁需要做什么

| 角色 | 需要做的事 |
| --- | --- |
| Ubuntu 部署人员 | 拉取代码、安装依赖、构建产物、执行 `up.sh` |
| HBuilderX CLI 使用者 | 通过脚本生成商城 H5 静态资源；可与 Ubuntu 部署人员是同一人 |
| Windows 使用者 | 通过 Ubuntu/虚拟机 IP 和前端端口访问系统，不需要安装 Podman |

构建产物（JAR、`Web/dist-prod`、商城 H5 目录、镜像 tar）均不会提交到 Git。每个部署环境都要自行构建，或从可信构建机复制产物。

## 2. 首次部署：Ubuntu 主机

### 2.1 拉取代码

```bash
git clone git@github.com:lcyspring/Group-11.git
cd Group-11
git pull --ff-only
```

后续更新只需要在仓库根目录执行：

```bash
git pull --ff-only
```

### 2.2 安装基础依赖

`install-build-deps-ubuntu.sh` 会安装 OpenJDK 17、Maven、Node.js 20、pnpm、Podman、rootless 网络组件和常用原生编译工具。脚本需要 sudo，但不会构建或启动项目。

```bash
cd podman
bash ./install-build-deps-ubuntu.sh
```

默认不使用宿主机代理。只有确实需要代理时才显式开启：

```bash
USE_HOST_PROXY=true bash ./install-build-deps-ubuntu.sh
```

## 3. 安装并配置 HBuilderX CLI

商城 H5 由官方 HBuilderX CLI 生成，要求 HBuilderX CLI 3.1.5+。

在 Ubuntu 上，`cli` 位于 HBuilderX 安装目录根目录。假设安装目录是 `/opt/HBuilderX`，可临时指定：

```bash
HBUILDERX_CLI=/opt/HBuilderX/cli bash ./build-mall-h5.sh --check
```

也可以将安装目录加入 `PATH`：

```bash
export PATH=/opt/HBuilderX:$PATH
cli ver
```

脚本会通过官方命令完成以下动作：启动 HBuilderX、打开 `MallFrontend` 项目、执行 `cli publish --platform h5`，并将生成的 `build/h5` 目录规范为 Podman 所需的 `build/web` 目录。

```bash
cd podman
HBUILDERX_CLI=/opt/HBuilderX/cli bash ./build-mall-h5.sh
```

成功后必须存在：

```text
MallFrontend/unpackage/dist/build/web/index.html
```

较新的 HBuilderX 4.67-alpha+ 可以直接使用 `web` 平台：

```bash
HBUILDERX_CLI=/opt/HBuilderX/cli HBUILDERX_PLATFORM=web \
  bash ./build-mall-h5.sh
```

如果 HBuilderX CLI 在 Windows 构建机上执行，请将生成的 `MallFrontend/unpackage/dist/build/web/` 整个目录复制到 Ubuntu 仓库的相同位置，再继续下面步骤。

## 4. 一次构建全部应用产物

先使用预检模式确认缺少什么；它不会下载依赖、编译或启动容器：

```bash
cd podman
HBUILDERX_CLI=/opt/HBuilderX/cli bash ./build-assets.sh --check --build-mall
```

预检成功后，一条命令构建后端、初始化服务、管理后台和商城 H5：

```bash
HBUILDERX_CLI=/opt/HBuilderX/cli bash ./build-assets.sh --build-mall
```

该命令依次生成：

```text
Server/mitedtsm-server/target/mitedtsm-server.jar
InitService/target/mitedtsm-init-service.jar
Web/dist-prod/
MallFrontend/unpackage/dist/build/web/
```

如果商城 H5 已经由其他构建机生成，可跳过 `--build-mall`：

```bash
bash ./build-assets.sh --check
bash ./build-assets.sh
```

## 5. 启动 Podman 服务

```bash
cd podman
bash ./up.sh
```

默认端口如下：

| 服务 | 主机端口 | 用途 |
| --- | ---: | --- |
| Server API | 8080 | 后端健康检查和 API |
| Web 管理后台 | 8081 | 管理员登录和管理页面 |
| Mall 商城 H5 | 8082 | 商城页面 |

需要改端口时，在启动命令前设置环境变量：

```bash
SERVER_PORT=18080 WEB_PORT=18081 MALL_PORT=18082 bash ./up.sh
```

`up.sh` 会构建运行镜像、创建 rootless Podman Pod、初始化数据库，并等待各服务健康后输出访问地址。

## 6. Windows 或虚拟机外部访问

先在 Ubuntu 上查看局域网 IP：

```bash
ip -br addr
```

假设 Ubuntu/虚拟机 IP 为 `192.168.1.50`，Windows 浏览器访问：

```text
管理后台：http://192.168.1.50:8081/
商城 H5：http://192.168.1.50:8082/
```

不要在 Windows 浏览器中把后端写成 `http://localhost:8080`：`localhost` 指向 Windows 自己，而不是 Ubuntu/虚拟机。管理后台已经使用同源 `/admin-api` 代理；登录请求应类似：

```text
http://192.168.1.50:8081/admin-api/...
```

若使用 VMware NAT，需要将宿主机端口转发到虚拟机的 8081/8082；若使用桥接网络，通常可直接使用虚拟机 IP。页面能打开但登录失败时，先在浏览器开发者工具的 Network 面板确认请求没有发往 `localhost:8080`，然后重新执行 Web 构建和 `up.sh`。

## 7. 代理和镜像来源

所有脚本默认清除 `http_proxy`、`https_proxy`、`all_proxy` 等宿主机代理变量；Podman 的 `build` 和 `run` 默认也使用 `--http-proxy=false`。

确实需要代理时，对单次命令显式开启：

```bash
USE_HOST_PROXY=true bash ./build-mall-h5.sh
USE_HOST_PROXY=true bash ./build-assets.sh --build-mall
USE_HOST_PROXY=true bash ./up.sh
```

镜像来源由 `IMAGE_SOURCE` 控制：

```bash
# 默认：有本地 docker-images/*.tar 时导入，否则拉取
IMAGE_SOURCE=auto bash ./up.sh

# 完全离线：必须准备好 docker-images/*.tar
IMAGE_SOURCE=archive bash ./up.sh

# 始终从镜像仓库拉取
IMAGE_SOURCE=pull bash ./up.sh
```

## 8. 日常更新流程

部署新版本前建议先停止旧 Pod，再拉取和构建：

```bash
cd podman
bash ./down.sh
cd ..
git pull --ff-only
cd podman
HBUILDERX_CLI=/opt/HBuilderX/cli bash ./build-assets.sh --build-mall
bash ./up.sh
```

`down.sh` 默认只停止 Pod，不会删除数据库等持久化卷。

## 9. 查看状态、日志和停止服务

```bash
# 查看 Pod 和容器状态
podman ps --pod

# 查看后端日志
podman logs --tail 200 mitedtsm-rootless-server

# 查看管理后台 Nginx 日志
podman logs --tail 200 mitedtsm-rootless-web

# 停止服务，保留数据库卷
cd podman && bash ./down.sh

# 停止服务并删除数据库、Redis、RabbitMQ、TDengine 数据卷（不可恢复）
cd podman && bash ./down.sh --volumes
```

## 10. 常见问题

| 现象 | 处理方式 |
| --- | --- |
| `HBuilderX CLI was not found` | 安装 HBuilderX CLI 3.1.5+，或设置 `HBUILDERX_CLI=/实际/cli`。 |
| `HBuilderX completed without the expected H5 output` | 检查 CLI 输出；确认项目能被 HBuilderX 打开，并检查 `MallFrontend/unpackage/dist/build/`。 |
| 缺少 Java、Maven、pnpm、Podman | 运行 `bash ./install-build-deps-ubuntu.sh`，再运行 `build-assets.sh --check --build-mall`。 |
| `Run this script as the normal rootless Podman user` | 不要用 sudo 运行 `up.sh`；用安装 Podman 的普通用户运行。 |
| 访问页面正常但登录报错 | 使用 Ubuntu/虚拟机 IP 加 `:8081`，确认请求地址是 `/admin-api/...` 而不是 Windows 的 `localhost:8080`；重新构建 Web。 |
| 拉取镜像失败 | 检查网络；需要代理时显式设置 `USE_HOST_PROXY=true`；离线环境准备 `docker-images/*.tar` 并使用 `IMAGE_SOURCE=archive`。 |

## 11. Git 与凭据规则

不要提交以下内容：

- `target/`、`Web/dist-prod/`、`MallFrontend/unpackage/`、`docker-images/*.tar` 等构建产物；
- 云厂商 AccessKey、API Key、短信密钥、真实密码；
- 本地环境文件或部署机私有配置。

初始 SQL 中不再提供云 API 的测试凭据。部署后需要在系统管理界面或部署机私有配置中录入自己的凭据，避免触发 GitHub Push Protection。

## 12. 最短可执行清单

```bash
git pull --ff-only
cd podman
bash ./install-build-deps-ubuntu.sh
HBUILDERX_CLI=/opt/HBuilderX/cli bash ./build-assets.sh --check --build-mall
HBUILDERX_CLI=/opt/HBuilderX/cli bash ./build-assets.sh --build-mall
bash ./up.sh
```
