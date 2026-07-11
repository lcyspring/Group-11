# Podman 全流程操作指南

本指南适用于团队成员在 Ubuntu 上构建并运行项目，以及在 Windows、虚拟机宿主机或局域网内访问系统。整个流程只使用 rootless Podman；不需要 Docker Engine、`docker` 命令、Docker Socket 或 Compose。

默认基础镜像名称中的 `docker.io` 只是 OCI 镜像仓库地址，并不表示需要安装或运行 Docker。镜像由 Podman 直接拉取；离线时也由 Podman 自己生成并加载 OCI 镜像归档。

## 1. 先明确谁需要做什么

| 角色 | 需要做的事 |
| --- | --- |
| Ubuntu 部署人员 | 拉取代码、安装依赖、构建产物、执行 `up.sh` |
| HBuilderX CLI 使用者 | 通过脚本生成商城 H5 静态资源；可与 Ubuntu 部署人员是同一人 |
| Windows 使用者 | 通过 Ubuntu/虚拟机 IP 和前端端口访问系统，不需要安装 Podman |

后端 JAR、`Web/dist-prod` 和镜像 tar 不会提交到 Git。当前商城 H5 的
`MallFrontend/unpackage/dist/build/web/` 已提交，成员拉取代码即可使用；只有
更新商城 H5 时才需要在有 HBuilderX 的机器上重新生成并提交该目录。

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

如果部署主机是 CachyOS（或 Arch 系），使用对应脚本：

```bash
cd podman
bash ./install-build-deps-cachyos.sh
```

它会安装 `jdk17-openjdk`、`maven`、`pnpm` 等缺失依赖，并使用已安装的
Pasta/`passt` 进行 rootless Podman 网络；不要求 `slirp4netns`。先仅查看缺失
依赖而不安装时可执行 `bash ./install-build-deps-cachyos.sh --check`。

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

如果 HBuilderX CLI 在 Windows 构建机上执行，请将生成的 `MallFrontend/unpackage/dist/build/web/` 整个目录复制到仓库相同位置，然后只提交并推送该目录；Ubuntu 部署机拉取后无需安装 HBuilderX。

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
bash ./up.sh --check
bash ./up.sh
```

`--check` 只检查 rootless Podman、端口参数和应用产物，绝不会拉取/加载镜像、构建镜像、创建容器或启动服务。首次部署建议先执行一次。

### 5.1 无变更时的快速启动

`bash ./up.sh` 仍用于部署新的 JAR、前端产物或 SQL。应用产物没有变化时，可以跳过
镜像打包，缩短日常启动时间：

```bash
# 已执行 down.sh，Pod 已删除但本地运行镜像仍在
bash ./up.sh --no-build

# Pod 还保留、只是被 podman pod stop 或主机重启停止
bash ./up.sh --fast
```

`--fast` 不重建或替换任何容器，只启动并检查原有 Pod；`--no-build` 用现有本地镜像
重新创建完整 Pod。两者都保留命名卷中的数据。需要发布新的构建产物时，使用不带参数的
`bash ./up.sh`。

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

`up.sh` 会构建运行镜像、创建 rootless Podman Pod、初始化数据库，并等待各服务健康后输出访问地址。基础设施健康检查、TDengine 初始化和前端启动会尽可能并行执行；Spring Boot 本身的初始化时间仍取决于应用和数据库数据量。

如果输出已经出现 `Spring Boot server is ready.`，但随后中断，且
`podman ps --pod` 中没有 `mitedtsm-rootless-web` 或
`mitedtsm-rootless-mall`，不需要重新构建、重启后端或删除数据。直接执行：

```bash
bash ./up.sh --frontends-only
```

该恢复模式只启动（或替换）两个 Nginx 前端容器；它不会打包镜像、重建 Pod、
重置数据库或删除任何卷。若仍失败，脚本会输出最后一次健康检查的实际错误。

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

镜像来源由 `IMAGE_SOURCE` 控制，所有离线归档均由 Podman 生成：

```bash
# 默认：有本地 podman/images/*.tar 时导入，否则由 Podman 拉取
IMAGE_SOURCE=auto bash ./up.sh

# 在有网络的 Podman 主机上创建可携带的 OCI 归档
bash ./image-archives.sh --pull

# 完全离线：复制 podman/images/*.tar 后加载，绝不访问镜像仓库
IMAGE_SOURCE=archive bash ./up.sh

# 始终由 Podman 从镜像仓库拉取
IMAGE_SOURCE=pull bash ./up.sh
```

若部署介质放在仓库外，可在两个命令上使用同一个绝对目录：

```bash
IMAGE_ARCHIVE_DIR=/mnt/deployment-images bash ./image-archives.sh --pull
IMAGE_ARCHIVE_DIR=/mnt/deployment-images IMAGE_SOURCE=archive bash ./up.sh
```

已有部署介质若仍使用仓库根目录的旧 `docker-images/`，可显式指定
`IMAGE_ARCHIVE_DIR=../docker-images`。Podman 可以直接加载其中的镜像归档，整个
过程仍不需要安装 Docker。

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

`down.sh` 默认停止并移除 Pod，但不会删除数据库等持久化卷。

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

# 默认最多等待 120 秒让 Java/Quartz 优雅退出；需要更久时自行调整
cd podman && STOP_TIMEOUT=300 bash ./down.sh

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
| `StopSignal SIGTERM failed ... resorting to SIGKILL` | 使用新版 `down.sh`；它默认等待 120 秒。仍超时时，查看后端日志并用 `STOP_TIMEOUT=300 bash ./down.sh` 增加优雅退出时间。 |
| 已显示 `Spring Boot server is ready.`，但没有 Web/Mall 容器 | 执行 `bash ./up.sh --frontends-only`；它只补启动前端，不重新构建或重启后端。 |
| 访问页面正常但登录报错 | 使用 Ubuntu/虚拟机 IP 加 `:8081`，确认请求地址是 `/admin-api/...` 而不是 Windows 的 `localhost:8080`；重新构建 Web。 |
| 拉取镜像失败 | 检查网络；需要代理时显式设置 `USE_HOST_PROXY=true`；离线环境在联网机器执行 `bash ./image-archives.sh --pull`，复制 `podman/images/*.tar` 后使用 `IMAGE_SOURCE=archive`。 |

## 11. Git 与凭据规则

不要提交以下内容：

- `target/`、`Web/dist-prod/`、`MallFrontend/unpackage/`、`podman/images/*.tar` 等构建产物；
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
bash ./up.sh --check
bash ./up.sh
```
