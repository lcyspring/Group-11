# Podman 镜像归档中文说明

本目录保存被 Git 忽略的 OCI tar 和 SHA-256 文件。

## 镜像来源与作用

### 外部基础镜像

| KDL 字段/镜像 | 上游来源 | 项目内作用 | 使用方式 |
|---|---|---|---|
| `image.runtime_base`：Temurin `17.0.19_10-jdk` + digest | Docker Hub 官方 Eclipse Temurin | `InitService` 与 Server 的 Java 基座 | 精确版本和 digest 双固定 |
| `image.mysql_base`：MySQL `8.0.46` + digest | Docker Hub 官方 MySQL | 直接承载数据库，部署期 stdin provision SQL | 精确版本和 digest 双固定 |
| `image.redis_base`：Redis `6.2.22-alpine` + digest | Docker Hub 官方 Redis | CRM/系统缓存 | 精确版本和 digest 双固定 |
| `image.rabbitmq_base`：RabbitMQ `3.13.7-management-alpine` + digest | Docker Hub 官方 RabbitMQ | 异步消息与管理探针 | 精确版本和 digest 双固定 |
| `image.tdengine_base`：TDengine `3.3.6.0` + digest | Docker Hub TDengine 官方仓库 | IoT 时序依赖；不是 CRM 真源 | 精确版本和 digest 双固定 |
| `image.nginx_base`：Nginx `1.30.0-alpine` + digest | Docker Hub 官方 Nginx | Web/Mall 静态运行镜像 | 精确版本和 digest 双固定 |
| 构建 `image.base`：`docker.io/library/ubuntu:26.04` | Docker Hub 官方 Ubuntu 镜像 | 两类统一编译工具链的操作系统基座 | 只用于构建镜像，不作为业务服务 |

这里的“官方”指对应上游在 Docker Hub 发布的镜像，不表示项目自行维护。版本来源完全由 KDL
指定；在线环境按 `image.source` 拉取，离线环境从本目录 OCI archive 加载。共享环境如需更严格的
供应链固定已使用可读精确标签与 sha256 digest 双引用；升级必须显式修改 KDL、重新验证并更新清单。

### 项目运行镜像

| 本地镜像 | 来源 | 作用 | 是否推荐长期 save |
|---|---|---|---|
| `localhost/mitedtsm-rootless-init-service:latest` | Temurin 17 + 当前 `InitService` JAR | 启动时执行系统初始化任务 | 否，优先由当前 JAR 重建 |
| `localhost/mitedtsm-rootless-server:latest` | Temurin 17 + 当前 Server JAR | 提供 8080 后端 API | 否，优先由当前 JAR 重建 |
| `localhost/mitedtsm-rootless-web:latest` | Nginx + 当前 `Web/dist-prod` | 提供 8081 管理端 | 否，优先由当前 Web 产物重建 |
| `localhost/mitedtsm-rootless-mall:latest` | Nginx + 当前 H5 构建产物 | 提供 8082 Mall H5 | 否，优先由当前 H5 产物重建 |

上述四个镜像由 `build-images.sh` 按 KDL 选择 `podman/Containerfile` 的不同 target 独立
封装；`deploy.sh` 只拉取/加载并消费它们，不再构建镜像。Redis、RabbitMQ 和 TDengine 没有再包一层
项目镜像；MySQL 同样直接使用官方镜像，但数据库 SQL 由 `deploy.sh` 在启动后从仓库通过 stdin
发送，不进入镜像或长期挂载。Podman 自动创建的 infra/pause 容器只负责共享 Pod 网络命名空间，也
不是可交付业务镜像。

### 编译工具链镜像

> 明确规则：普通成员编译项目时必须使用下表两个 `ghcr.io/elel-code` 公共镜像，不需要先构建镜像。
> `docker.io/library/ubuntu:26.04` 只是维护者重建工具链镜像时的基座，不是日常项目编译入口。

| 本地/仓库镜像 | 构建来源 | 作用 | 交付建议 |
|---|---|---|---|
| `ghcr.io/elel-code/group-11-build-ubuntu:26.04-deno-2.9.3` | Ubuntu 26.04 + OpenJDK 17.0.19 + Maven 3.9.12 + Deno 2.9.3；Deno 二进制来自官方固定摘要层，镜像不含 Node/npm/pnpm | 编译 Server、InitService、Web，执行 Deno Test/覆盖率，并在容器运行时安装 Web/Mall 依赖 | public；版本标签摘要为 `sha256:29c77cc02e5fcf5fa2d77bf2bd527b0bb0fdc435d7bb6b12bc56e8e524ccf47d`，离线环境使用带 SHA-256 的 OCI tar |
| `ghcr.io/elel-code/group-11-hbuilderx-ubuntu:26.04-5.05` | Ubuntu 26.04 + HBuilderX 5.05 提取出的 Node、uni-app Vite 和 Sass 无图形组件 | 挂载 Mall 依赖卷并断网编译 H5 | 已公开，脚本默认优先拉取；离线环境使用带 SHA-256 的 OCI tar |

HBuilderX 工具链镜像不是从 Docker Hub 下载的现成镜像。普通成员直接使用上面的公共 GHCR image；
仅工具链维护者在 KDL 显式设置 `image.rebuild: true` 时，才从 `hbuilderx.source_dir` 读取团队有权使用的 HBuilderX 安装目录。GHCR 中两个镜像是本项目已经构建的
编译制品，不是上游官方镜像。`elel-code` 命名空间下这两个 package 当前均为 public，其他成员 pull
不需要登录；只有维护者 push 新标签时才需要 `podman login ghcr.io`。成员可直接 pull/load，通常无需
在宿主安装 JDK、Deno、Node、npm、pnpm 或 HBuilderX。

`ghcr.io/elel-code/group-11-build-ubuntu:26.04` 是同一镜像的稳定别名，2026-07-19 已更新到上述
摘要；共享编译 KDL 仍使用带 Deno 版本的标签，保证升级需要显式修改配置。

## 哪些镜像需要归档

- 编译工具链镜像重建慢、版本敏感，推荐同时上传 GHCR 和执行 `operations/images/build-image-archives.sh save`；
- 六个外部基础镜像仅在离线交付时执行 `operations/images/image-archives.sh save/pull-save`；
- 四个项目运行镜像和四个直接运行的基础服务镜像由 KDL、源码产物及上游来源共同复现，日常不建议
  用长期 tar 取代源码与构建记录；
- 常规基础镜像归档使用 OCI tar；完整离线交付包为兼容 Podman 与 Docker，统一使用带稳定标签的
  `docker-archive`。两者都生成 SHA-256，加载或交接前必须校验。

- `operations/images/image-archives.sh`：保存 JDK、MySQL、Redis、RabbitMQ、TDengine、Nginx 等运行基础镜像，主要用于
  离线部署；
- `operations/images/build-offline-deployment-bundle.sh`：从本机运行 KDL 导出目标机真正需要的 MySQL、Redis、
  RabbitMQ、TDengine、InitService、Server、Web、Mall 八个双引擎镜像，并汇总数据库清单、BPM 配置、dasel、
  生产默认无演示数据配置以及 Podman/Docker Compose 一键脚本；构建机基础设施凭据不会写入交付模板；
- `operations/images/build-image-archives.sh`：保存/加载/上传 Ubuntu 26.04 Server-Web 与无图形 HBuilderX 编译镜像，
  推荐用于成员环境统一；
- 项目 Server/Web/Mall 运行镜像默认从当前源码产物重建，不建议用长期 tar 代替源码版本管理；
- tar 和 checksum 不提交 Git，需要共享时放入制品库或 OCI 镜像仓库。

候选上传目标由 KDL 显式指定。public 镜像下载不要求登录；上传 GHCR 前仍须由维护者本人完成
`podman login ghcr.io`，脚本不会读取或保存 GitHub Token。
