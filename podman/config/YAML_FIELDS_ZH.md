# Podman YAML 字段参考

返回：[Podman 中文 README](../README_ZH.md)；配置分类见 [README_ZH.md](README_ZH.md)。

所有入口命令行只接收一个 YAML 路径。解析器只支持“顶层分组 + 一层标量”，不支持数组、锚点、
多行值或第三层映射；逗号分隔值仍是一个标量。

## 运行配置

### 根与操作

| 字段 | 作用 |
|---|---|
| `schema_version` | 配置协议版本，当前固定为 `1` |
| `operation.startup_mode` | `check/full/fast/no-build/frontends-only/rebuild-server/rebuild-web/rebuild-mall` |
| `operation.shutdown_mode` | `check` 只预检，`stop` 停止 Pod |
| `operation.archive_mode` | `check/save/pull-save`，供镜像归档脚本使用 |
| `operation.remove_volumes_on_down` | 停止时是否永久删除四个数据卷；默认必须为 `false` |
| `deployment.pod_name` | rootless Pod 名称 |
| `deployment.stop_timeout_seconds` | Server 优雅停止最大等待秒数 |

### 网络

| 字段 | 作用 |
|---|---|
| `network.host_address` | 发布端口绑定地址；本地观察使用 `127.0.0.1` |
| `network.*_host_port` | Server/Web/Mall 的宿主端口 |
| `network.*_container_port` | Pod 内对应端口 |
| `network.use_host_proxy` | 是否把显式代理注入 build/run；不会读取宿主隐式环境 |
| `network.host_proxy_name` | 容器访问宿主代理时替换 loopback 的主机名 |
| `network.http_proxy/https_proxy/all_proxy` | 代理 URL；不用时写 `none` |
| `network.no_proxy` | 不经过代理的逗号分隔地址 |

### 镜像与归档

| 字段 | 作用 |
|---|---|
| `image.source` | `auto` 优先本地/归档再拉取，`archive` 仅离线归档，`pull` 强制仓库 |
| `image.archive_dir` | 归档目录，相对 YAML 文件解析 |
| `image.*_base` | JDK、MySQL、Redis、RabbitMQ、TDengine、Nginx 基础 OCI 镜像 |
| `image.*_runtime` | 项目打包后的 MySQL/Init/Server/Web/Mall 本地镜像名 |
| `archive.*` | 各基础镜像对应的 tar 文件名 |

### 容器、卷和基础设施

| 字段 | 作用 |
|---|---|
| `container.*` | 八个容器的稳定名称，供启动、探针和验收脚本引用 |
| `volume.mysql/redis/rabbitmq/tdengine` | 四个持久数据卷名称 |
| `mysql.database` | 主业务库名 |
| `mysql.root_password` | 本地 MySQL root 密码；真实值只能在忽略的本机 YAML |
| `mysql.character_set/collation` | 建库、客户端和迁移字符集/排序规则 |
| `mysql.authentication_plugin` | MySQL 认证插件 |
| `mysql.timezone` | MySQL 容器时区 |
| `mysql.compatibility_migration_manifest` | 已有数据卷每次部署前执行的幂等迁移清单 |
| `rabbitmq.username/password` | RabbitMQ 运行账号 |
| `tdengine.host/port/fqdn` | 应用访问 TDengine 及容器 FQDN 配置 |
| `tdengine.username/password` | TDengine 数据源账号 |
| `tdengine.initialization_attempts` | InitService 初始化 TDengine 的最大尝试次数 |
| `server.spring_profile` | Server 激活的 Spring profile |

### 安全与集成

| 字段 | 作用 |
|---|---|
| `security.mock_login_enabled/mock_secret` | Mock 登录开关和显式密钥；默认关闭 |
| `security.password_encoder_length` | BCrypt 强度，允许 10～16 |
| `security.xss_enabled` | XSS 过滤总开关 |
| `security.cors_allowed_origins` | 可信 Origin，逗号分隔；凭证模式禁止 `*` |
| `security.cors_allowed_headers/methods` | CORS 请求头和方法白名单 |
| `security.cors_allow_credentials` | 是否允许跨域凭证 |
| `security.cors_max_age_seconds` | 浏览器预检缓存秒数 |
| `security.api_docs_enabled` | OpenAPI、Swagger UI、Knife4j 总开关 |
| `security.druid_console_enabled` | Druid Web 监控开关 |
| `security.actuator_exposure` | Actuator 暴露端点，本地安全基线为 `health,info` |
| `security.api_encryption_enabled` | API 加密协议开关；无正式密钥与前端协议时关闭 |
| `security.captcha_enabled` | 登录验证码开关；自动验收可显式关闭 |
| `security.boot_admin_client_enabled` | Spring Boot Admin 客户端开关 |
| `integration.justauth_enabled` | 第三方社交登录总开关 |
| `integration.wx_*` | 公众号和小程序 App ID/Secret |
| `integration.tencent_lbs_key` | 腾讯地图密钥 |
| `integration.pay_*_notify_url` | 支付订单、退款、转账回调 URL，必须为 HTTP(S) |
| `integration.express_client` | 快递 Provider；未提供时用 `not_provide` |
| `integration.express_kdniao_*` | 快递鸟业务 ID、密钥与请求类型 |
| `integration.express_kd100_*` | 快递 100 key/customer |

### 文件与健康检查

| 字段 | 作用 |
|---|---|
| `file.storage_mode` | 当前支持 `database`，选择数据库文件配置 |
| `file.client_id` | `infra_file_config` 中作为 master 的配置 ID |
| `file.public_base_url` | 文件公开基地址，只接受 HTTP(S) |
| `health.http_host` | 宿主健康探针地址 |
| `health.interval_seconds` | 重试间隔 |
| `health.*_attempts` | 各服务最大探测次数 |
| `health.mysql_host/mysql_user` | MySQL 探针连接信息 |
| `health.mysql_schema_query` | 判断基础 schema 就绪的只读 SQL |
| `health.rabbitmq_os_user` | 执行 RabbitMQ 诊断命令的容器用户 |
| `health.tdengine_query` | TDengine 就绪 SQL |
| `health.server_path/web_path/mall_path` | 三个应用的健康路径 |

## Ubuntu 26.04 Server/Web 构建配置

| 字段 | 作用 |
|---|---|
| `image.base/name/rebuild` | Ubuntu 基础镜像、专用构建镜像名、是否重建工具链镜像 |
| `toolchain.pnpm_version` | 容器内 pnpm 版本 |
| `build.server/init_service/web` | 是否构建对应产物 |
| `build.clean` | Maven/Web 是否清理旧产物 |
| `build.crm_tests/crm_coverage` | 是否执行 CRM 测试和 JaCoCo |
| `build.ci` | 使用 CI 行为和非交互输出 |
| `build.maven_threads` | Maven reactor 并发线程 |
| `build.pnpm_frozen_lockfile` | 是否禁止 lockfile 漂移 |
| `network.use_host_proxy` | 构建是否使用显式代理策略 |
| `web.baidu_analytics_code` | 管理端百度统计代码；`disabled` 表示不注入 |
| `web.legacy_media_origins` | 退休媒体源列表，构建时归一化为本地代理/回退 |
| `web.test_script` | Web 构建前附加测试脚本，可省略 |
| `cache.*` | Maven、pnpm store、node_modules 缓存卷与容器路径 |
| `runtime.memory/cpus` | 构建容器资源上限 |

## Mall H5 构建配置

| 字段 | 作用 |
|---|---|
| `image.base/name/rebuild` | Ubuntu 基础镜像、无图形 HBuilderX 镜像名和重建开关 |
| `hbuilderx.source_dir` | 仅重建镜像时读取的宿主 HBuilderX 目录 |
| `build.platform` | 当前固定 `h5` |
| `build.clean_output` | 构建前是否清空 H5 输出 |
| `media.legacy_origins` | H5 退休远程媒体源 |
| `media.legacy_fallback` | 无法代理媒体的本地回退资源 |
| `network.mode` | H5 构建容器网络；可复现构建使用 `none` |
| `runtime.memory/cpus` | H5 构建容器资源上限 |

## CRM MySQL 备份恢复配置

| 字段 | 作用 |
|---|---|
| `operation.mode` | 备份脚本使用 `check/backup`，恢复脚本使用 `check/restore` |
| `container.mysql/server` | 目标 MySQL 与需要停机保护的 Server 容器 |
| `mysql.database/username/password` | CRM 真源库及备份账号；真实密码只在 ignored YAML |
| `archive.directory/filename` | `.sql.gz` 与同名 `.sha256` 的目录和文件名 |
| `archive.overwrite` | 备份是否允许覆盖已有归档 |
| `restore.target_database` | 恢复目标库；演练必须使用隔离库名 |
| `restore.allow_replace` | 是否允许删除并重建已存在的目标库 |
| `restore.allow_live_database_replace` | 目标等于运行真源库时的第二道显式授权 |
| `restore.drop_after_verify` | 验证核心表和表数后是否删除演练库 |

## 编译工具链镜像归档配置

| 字段 | 作用 |
|---|---|
| `operation.mode` | `check/save/load/push` |
| `registry.host` | push 的唯一允许仓库主机 |
| `image.*_builder_source` | 本机 Ubuntu Server/Web 与 HBuilderX 编译镜像 |
| `image.*_builder_destination` | GHCR 等 OCI 仓库的版本标签 |
| `archive.directory` | OCI tar 与 SHA-256 输出目录 |
| `archive.*_builder_filename` | 两个编译镜像归档文件名 |
| `archive.overwrite` | 是否允许覆盖已存在归档 |
