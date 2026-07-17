# Podman 镜像归档中文说明

本目录保存被 Git 忽略的 OCI tar 和 SHA-256 文件。

## 镜像来源与作用

### 外部基础镜像

| YAML 字段/镜像 | 上游来源 | 项目内作用 | 使用方式 |
|---|---|---|---|
| `image.runtime_base`：`docker.io/library/eclipse-temurin:17-jdk` | Docker Hub 官方镜像库的 Eclipse Temurin JDK 17 | `InitService` 与 Server 项目运行镜像的 Java 基座 | `up.sh full` 构建项目镜像时读取 |
| `image.mysql_base`：`docker.io/library/mysql:8.0` | Docker Hub 官方 MySQL 镜像 | 加入初始化入口和 `database/` SQL，生成项目 MySQL 镜像 | 不直接启动，先构建 `mysql_runtime` |
| `image.redis_base`：`docker.io/library/redis:6-alpine` | Docker Hub 官方 Redis 镜像 | CRM/系统缓存 | 作为运行容器直接启动 |
| `image.rabbitmq_base`：`docker.io/library/rabbitmq:3-management-alpine` | Docker Hub 官方 RabbitMQ 镜像 | 异步消息与管理探针 | 作为运行容器直接启动 |
| `image.tdengine_base`：`docker.io/tdengine/tdengine:3.3.6.0` | Docker Hub TDengine 官方仓库 | IoT 时序数据依赖；不是 CRM 业务真源 | 作为运行容器直接启动 |
| `image.nginx_base`：`docker.io/library/nginx:stable-alpine` | Docker Hub 官方 Nginx 镜像 | 分别承载 Web 与 Mall 静态产物，并反代管理 API | 构建两个独立前端运行镜像 |
| 构建 `image.base`：`docker.io/library/ubuntu:26.04` | Docker Hub 官方 Ubuntu 镜像 | 两类统一编译工具链的操作系统基座 | 只用于构建镜像，不作为业务服务 |

这里的“官方”指对应上游在 Docker Hub 发布的镜像，不表示项目自行维护。版本来源完全由 YAML
指定；在线环境按 `image.source` 拉取，离线环境从本目录 OCI archive 加载。共享环境如需更严格的
供应链固定，可把 YAML 标签升级为 digest 引用，脚本不会隐式替换来源。

### 项目运行镜像

| 本地镜像 | 来源 | 作用 | 是否推荐长期 save |
|---|---|---|---|
| `localhost/mitedtsm-rootless-mysql:latest` | MySQL 8.0 基础镜像 + `database/` 生命周期 SQL + 初始化脚本 | 创建和迁移项目数据库 | 否，优先由当前 SQL 重建 |
| `localhost/mitedtsm-rootless-init-service:latest` | Temurin 17 + 当前 `InitService` JAR | 启动时执行系统初始化任务 | 否，优先由当前 JAR 重建 |
| `localhost/mitedtsm-rootless-server:latest` | Temurin 17 + 当前 Server JAR | 提供 8080 后端 API | 否，优先由当前 JAR 重建 |
| `localhost/mitedtsm-rootless-web:latest` | Nginx + 当前 `Web/dist-prod` | 提供 8081 管理端 | 否，优先由当前 Web 产物重建 |
| `localhost/mitedtsm-rootless-mall:latest` | Nginx + 当前 H5 构建产物 | 提供 8082 Mall H5 | 否，优先由当前 H5 产物重建 |

上述五个镜像由 `podman/Containerfile` 的不同 target 构建。Redis、RabbitMQ 和 TDengine 没有再包一层
项目镜像；Podman 自动创建的 infra/pause 容器只负责共享 Pod 网络命名空间，也不是可交付业务镜像。

### 编译工具链镜像

| 本地/仓库镜像 | 构建来源 | 作用 | 交付建议 |
|---|---|---|---|
| `ghcr.io/elel-code/group-11-build-ubuntu:26.04` | Ubuntu 26.04 + OpenJDK 17 + Maven + Node.js + pnpm 11.3.0 + 项目构建入口 | 编译 Server、InitService、Web，并在运行容器中安装 Web/Mall 依赖 | 已公开，脚本默认优先拉取；离线环境使用带 SHA-256 的 OCI tar |
| `ghcr.io/elel-code/group-11-hbuilderx-ubuntu:26.04-5.05` | Ubuntu 26.04 + HBuilderX 5.05 提取出的 Node、uni-app Vite 和 Sass 无图形组件 | 挂载 Mall 依赖卷并断网编译 H5 | 已公开，脚本默认优先拉取；离线环境使用带 SHA-256 的 OCI tar |

HBuilderX 工具链镜像不是从 Docker Hub 下载的现成镜像。普通成员直接使用上面的公共 GHCR image；
仅工具链维护者在 YAML 显式设置 `image.rebuild: true` 时，才从 `hbuilderx.source_dir` 读取团队有权使用的 HBuilderX 安装目录。GHCR 中两个镜像是本项目已经构建的
编译制品，不是上游官方镜像。`elel-code` 命名空间下这两个 package 当前均为 public，其他成员 pull
不需要登录；只有维护者 push 新标签时才需要 `podman login ghcr.io`。成员可直接 pull/load，通常无需
在宿主安装 JDK、pnpm 或 HBuilderX。

## 哪些镜像需要归档

- 编译工具链镜像重建慢、版本敏感，推荐同时上传 GHCR 和执行 `build-image-archives.sh save`；
- 六个外部基础镜像仅在离线交付时执行 `image-archives.sh save/pull-save`；
- 五个项目运行镜像和三个直接运行的基础服务镜像由 YAML、源码产物及上游来源共同复现，日常不建议
  用长期 tar 取代源码与构建记录；
- 所有 OCI tar 均生成同名 SHA-256，加载或交接前必须校验。

- `image-archives.sh`：保存 JDK、MySQL、Redis、RabbitMQ、TDengine、Nginx 等运行基础镜像，主要用于
  离线部署；
- `build-image-archives.sh`：保存/加载/上传 Ubuntu 26.04 Server-Web 与无图形 HBuilderX 编译镜像，
  推荐用于成员环境统一；
- 项目 Server/Web/Mall 运行镜像默认从当前源码产物重建，不建议用长期 tar 代替源码版本管理；
- tar 和 checksum 不提交 Git，需要共享时放入制品库或 OCI 镜像仓库。

候选上传目标由 YAML 显式指定。public 镜像下载不要求登录；上传 GHCR 前仍须由维护者本人完成
`podman login ghcr.io`，脚本不会读取或保存 GitHub Token。
