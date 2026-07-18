# Podman YAML 字段参考

返回：[Podman 中文 README](../README_ZH.md)；配置分类见 [README_ZH.md](README_ZH.md)。

所有入口命令行只接收一个 YAML 路径。解析器只支持“顶层分组 + 一层标量”，不支持数组、锚点、
多行值或第三层映射；逗号分隔值仍是一个标量。

## 编译配置

`compile.sh` 是唯一公开编译入口，Server、InitService、Web 和 Mall H5 不再各自暴露脚本。目标
选择使用显式白名单减黑名单；黑名单优先，不根据配置文件名、Host 已安装程序或环境变量猜测。

| 字段 | 作用 |
|---|---|
| `build.include_targets` | `all`、`none`，或 `server,init-service,web,mall-h5` 子集；构成编译白名单 |
| `build.exclude_targets` | 同一取值协议；从白名单中排除目标，冲突时本字段优先 |
| `image.standard` | Server、InitService、Web 和测试使用的公开 Ubuntu 26.04 工具链镜像 |
| `image.hbuilderx` | Mall H5 使用的公开无图形 HBuilderX Ubuntu 26.04 镜像 |
| `image.dependency` | Mall 项目依赖在容器运行时下载所用的 Node/pnpm 工具链镜像 |
| `image.rebuild` | 日常固定 `false`；仅工具链维护者发布新镜像时显式开启 |
| `cache.*` | Maven、pnpm store 和 node_modules 的 Podman 命名卷；依赖不落到 Host |
| `network.use_host_proxy` | 是否将显式 Host 代理传给依赖下载容器；默认不继承 Host 代理 |

例如 `include_targets: all` 加 `exclude_targets: mall-h5` 构建三个标准产物；
`include_targets: web` 加 `exclude_targets: none` 只构建管理端；两者为 `all/none` 时可一次执行四个
目标。`*_tests/*_coverage` 只控制测试门禁，不属于产物白名单。项目依赖在容器运行时准备，工具链
镜像构建阶段不内置项目 `node_modules`。

## 运行配置

### 根与操作

| 字段 | 作用 |
|---|---|
| `schema_version` | 配置协议版本，当前固定为 `1` |
| `operation.startup_mode` | `check/replace/fast/frontends-only/replace-server/replace-web/replace-mall`；只控制容器，不触发镜像构建 |
| `operation.shutdown_mode` | `check` 只预检，`stop` 停止 Pod |
| `operation.archive_mode` | `check/save/pull-save`，供镜像归档脚本使用 |
| `operation.remove_volumes_on_down` | 停止时是否永久删除四个数据卷；默认必须为 `false` |
| `operation.confirm_persistent_data_reset` | 删除持久卷的第二道确认；必须与 `remove_volumes_on_down` 同为 `true`，日常必须为 `false` |
| `deployment.pod_name` | rootless Pod 名称 |
| `deployment.stop_timeout_seconds` | Server 优雅停止最大等待秒数 |

### 网络

| 字段 | 作用 |
|---|---|
| `network.host_address` | Server/Web/Mall 发布端口绑定地址；团队验收示例为 `0.0.0.0`，并由 UFW/安全组限制来源 |
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
| `image.redis_base/rabbitmq_base/tdengine_base/mysql_base` | 直接运行的四个基础服务镜像；MySQL 使用官方镜像，不再生成项目 MySQL 镜像 |
| `image.init_runtime/server_runtime/web_runtime/mall_runtime` | 项目打包后的四个应用运行镜像名 |
| `archive.*` | 启动阶段所需基础镜像和项目运行镜像的 tar 文件名 |

### 运行镜像封装配置

`build-images.sh` 只接收一个 YAML 路径，不调用编译脚本或 `deploy.sh`。

| 字段 | 作用 |
|---|---|
| `operation.mode` | `check` 只校验产物和配置，`package` 执行运行镜像封装 |
| `build.targets` | `all`，或 `init-service,server,web,mall` 的逗号分隔子集 |
| `build.containerfile` | 多阶段运行镜像 Containerfile；相对 YAML 文件解析 |
| `network.use_host_proxy` | 是否使用本 YAML 的显式代理下载缺失基础镜像 |
| `network.http_proxy/https_proxy/all_proxy` | Host 侧拉取镜像的代理 URL；不用写 `none` |
| `network.no_proxy` | 不走代理的地址列表 |
| `image.source` | 基础镜像来源：`auto/archive/pull` |
| `image.archive_dir` | 基础镜像离线归档目录 |
| `image.runtime_base/nginx_base` | Java 与 Nginx 封装基座 |
| `image.init_runtime/server_runtime/web_runtime/mall_runtime` | 四个产出镜像的完整名称和标签 |
| `archive.runtime_base/nginx_base` | 两种封装基座的 OCI tar 文件名 |

### 容器、卷和基础设施

| 字段 | 作用 |
|---|---|
| `container.*` | 八个容器的稳定名称，供启动、探针和验收脚本引用 |
| `volume.mysql/redis/rabbitmq/tdengine` | 四个持久数据卷名称 |
| `mysql.database` | 主业务库名 |
| `mysql.dataset` | 仅空数据卷初始化时选择的数据集；切换它不会修改已有持久卷 |
| `mysql.bootstrap_policy` | `initialize-empty` 只初始化确认空库；`require-existing` 拒绝空库 |
| `mysql.bootstrap_manifest` | 空库建表、必要基础数据和显式种子的执行清单；SQL 在部署期通过 stdin 发送 |
| `mysql.root_password` | 本地 MySQL root 密码；真实值只能在忽略的本机 YAML |
| `mysql.character_set/collation` | 建库、客户端和迁移字符集/排序规则 |
| `mysql.authentication_plugin` | MySQL 认证插件 |
| `mysql.timezone` | MySQL 容器时区 |
| `mysql.compatibility_migration_manifest` | 已有数据卷每次部署前执行的幂等迁移清单 |

### 已有数据库的数据集替换配置

`operations/database/database-dataset.sh` 只接收一个 YAML 路径，不由 `deploy.sh` 自动调用。

| 字段 | 作用 |
|---|---|
| `operation.dataset_mode` | `check` 只校验；`replace` 才执行持久数据替换 |
| `container.mysql` | 目标 MySQL 容器名 |
| `mysql.database/username/password` | 目标库与执行账号；真实密码只写 ignored YAML |
| `mysql.dataset` | 要应用的显式数据集 manifest 名称 |
| `mysql.cleanup_existing_before_dataset` | 是否允许 manifest 执行 cleanup SQL；必须与 manifest 内容一致 |
| `mysql.confirm_persistent_data_change` | `replace` 模式修改持久数据的第二道显式确认 |
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
| `crm_marketing.provider_mode` | 群发提供商模式；`record-only` 只留痕，`system` 调用系统短信/邮件提供商 |
| `crm_marketing.tracking_enabled` | 是否为系统邮件追加不可猜测的打开追踪像素 |
| `crm_marketing.public_base_url` | 邮件像素公开基地址；真实邮件必须使用外部可访问的 HTTPS 地址 |
| `crm_marketing.delivery_sync_batch_size` | 每轮调度最多回收的短信/邮件提供商结果数 |
| `crm_marketing.click_tracking_enabled` | 是否生成逐收件人营销跳转令牌并记录点击；关闭时禁止保存带跟踪链接的群发 |
| `crm_marketing.click_allowed_hosts` | 营销目标域名白名单，逗号分隔；支持 `*.example.com`，不允许把目标 URL 作为匿名接口参数 |
| `crm_marketing.max_links_per_broadcast` | 单个群发允许配置的营销跟踪链接上限 |
| `marketing_provider.provision_mode` | `disabled` 不改库；`create-only` 只补稳定键缺项；`managed` 由 YAML 幂等更新受管项 |
| `marketing_provider.sms_enabled/mail_enabled` | 是否 provision 短信或邮件聚合；非 disabled 至少开启一项，并要求 `crm_marketing.provider_mode: system` |
| `marketing_provider.sms_channel_code` | System 模块支持的短信渠道编码：`ALIYUN/TENCENT/HUAWEI/QINIU/DEBUG_DING_TALK` |
| `marketing_provider.sms_signature/api_key/api_secret/callback_url` | 短信签名、账号秘密及可选回调；启用短信时占位值会被拒绝 |
| `marketing_provider.sms_template_*` | 短信模板稳定编码、名称、内容、JSON 参数数组和供应商模板 ID |
| `marketing_provider.mail_address/username/password/host/port` | 邮件账号稳定地址和 SMTP 连接；启用邮件时占位账号会被拒绝 |
| `marketing_provider.mail_ssl_enabled/mail_starttls_enabled` | SMTP 加密方式，SSL 与 STARTTLS 不允许同时开启 |
| `marketing_provider.mail_template_*` | 邮件模板稳定编码、名称、昵称、标题、HTML 内容和 JSON 参数数组 |
| `crm_customer_import.max_rows` | 单次客户导入预检允许的数据行上限，防止超大文件占满内存 |
| `crm_customer_import.preview_ttl_minutes` | 预检快照允许确认的分钟数，过期后必须重新预检 |
| `crm_export_task.enabled` | 是否启用 CRM 异步导出后台任务 |
| `crm_export_task.batch_size` | 每次租户调度领取的排队任务数 |
| `crm_export_task.max_batch_size` | 调度和过期清理的硬上限，且 `batch_size` 不得超过它 |
| `crm_export_task.max_pending_per_user` | 单用户允许同时排队或运行的任务数 |
| `crm_export_task.max_rows` | 单任务允许冻结和导出的最大对象行数 |
| `crm_export_task.retention_hours` | 任务、受保护结果文件和重新下载能力的保留小时数 |
| `crm_export_task.token_ttl_seconds` | 单次下载令牌的有效秒数；每个令牌成功下载后立即失效 |
| `crm_export_task.cron` | 后台导出与过期清理的 Quartz cron |
| `crm_export_task.zone` | cron 时区 |
| `crm_export_task.lock_key` | 多实例调度使用的 Redisson 分布式锁键 |
| `crm_export_task.lock_lease_seconds` | 调度锁最长持有秒数 |
| `health.http_host` | 宿主健康探针地址 |
| `health.interval_seconds` | 重试间隔 |
| `health.*_attempts` | 各服务最大探测次数 |
| `health.mysql_host/mysql_user` | MySQL 探针连接信息 |
| `health.mysql_schema_query` | 判断基础 schema 就绪的只读 SQL |
| `health.rabbitmq_os_user` | 执行 RabbitMQ 诊断命令的容器用户 |
| `health.tdengine_query` | TDengine 就绪 SQL |
| `health.server_path/web_path/mall_path` | 三个应用的健康路径 |
| `bpm.provision_after_start` | Server 就绪后是否根据显式清单创建/更新并部署全部受管 BPM 模型；全新数据库卷应设为 `true` |
| `bpm.provision_manifest` | BPM 聚合清单路径；清单再引用各模型的 ignored local YAML，真实账号不写入运行脚本或仓库 |

## BPM 模型恢复配置

单模型示例为 `bpm-provision*.example.yaml`，聚合示例为
`bpm-provision-all.example.yaml`。示例必须保留在 Git 中，真实凭据仅写入 ignored
`*-local.yaml`。

| 分组/字段 | 作用 |
|---|---|
| `schema_version` | 配置协议版本，当前固定为 `1` |
| `endpoint.base_url` | 管理端 API 根地址，通常为 `http://127.0.0.1:8080/admin-api` |
| `endpoint.tenant_id` | 创建角色、分类和模型所属租户 |
| `account.username/password` | 执行恢复的管理员账号；示例密码必须为占位值 |
| `approval.role_code/role_name/role_sort` | 审批角色的稳定编码、显示名和排序 |
| `approval.approver_username` | 被授予审批角色并绑定到流程节点的用户 |
| `approval.permission_codes` | 审批角色需要的权限编码，使用逗号分隔 |
| `category.code/name/sort` | BPM 流程分类的稳定编码、名称和排序 |
| `model.key` | Flowable 流程定义 key，必须与业务提交审批时使用的 key 完全一致 |
| `model.name/description` | 模型显示名和业务边界说明 |
| `model.form_create_path/form_view_path` | 发起页和详情页前端路由 |
| `model.approval_node_name` | 审批任务节点显示名 |
| `models.*` | 聚合清单内各受管模型配置路径，相对聚合 YAML 所在目录解析 |

单模型恢复使用 `operations/bpm/provision-bpm-model.sh`，全模型恢复使用
`operations/bpm/provision-bpm-models.sh`；两者命令行都只能传一个 YAML 路径。聚合恢复任一模型失败时整体返回
失败，防止部署表面成功但提交审批仍报“流程定义不存在”。

## 清理配置

| 示例 | 数据影响 |
|---|---|
| `cleanup-stop.example.yaml` | 删除运行 Pod，四个 named volume 全部保留 |
| `cleanup-reset.example.yaml` | 删除运行 Pod，并永久删除四个 named volume |

`stop.sh` 只读取 `schema_version`、`operation.shutdown_mode`、
`operation.remove_volumes_on_down`、`operation.confirm_persistent_data_reset`、`deployment.*`、
`container.server` 和 `volume.*`。两个删除确认必须显式填写且值保持一致；日常均为 `false`。只有已备份
且确实需要空数据库、空缓存、空消息队列和空时序库时才复制 reset 示例，并将两者同时改为 `true`。

## Ubuntu 26.04 Server/Web 构建配置

| 字段 | 作用 |
|---|---|
| `image.base/standard/rebuild` | Ubuntu 基础镜像、标准工具链镜像名、是否重建工具链镜像 |
| `toolchain.pnpm_version` | 容器内 pnpm 版本 |
| `build.include_targets/exclude_targets` | 四目标白名单与黑名单；标准容器只消费最终选中的前三个目标 |
| `build.clean` | Maven/Web 是否清理旧产物 |
| `build.crm_tests/crm_coverage` | 是否执行 CRM 测试和 JaCoCo |
| `build.framework_tests/framework_coverage` | 是否执行显式选择的 Framework 模块测试和 JaCoCo |
| `build.framework_modules` | Framework Maven 模块路径，多个模块用逗号分隔；未配置时为 Security/Web 组合 |
| `build.framework_test_pattern` | Framework Surefire 测试类模式，不接受任意命令 |
| `build.system_tests/system_coverage` | 是否执行指定 System 模块测试和 JaCoCo |
| `build.system_test_pattern` | System 模块 Surefire 测试类模式，不接受任意命令 |
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

`compile.sh` 同样只接受一个 YAML 路径。项目依赖不在 Host
执行安装，也不写入工具链 image：先由 `image.dependency` 指定的 Ubuntu 26.04
工具容器在运行时执行 pnpm，写入 `cache.mall_node_modules_volume` 与
`cache.mall_pnpm_store_volume`；之后 HBuilderX 容器挂载同一 `node_modules` 卷并以
`network.mall_mode: none` 编译。Host 的 `MallFrontend/node_modules` 不参与构建。

| 字段 | 作用 |
|---|---|
| `image.hbuilderx` | 无图形 HBuilderX 编译工具链 image；固定使用公开的 `ghcr.io/elel-code/group-11-hbuilderx-ubuntu:26.04-5.05`。 |
| `image.dependency` | 运行时安装 Mall 项目依赖的 Ubuntu 26.04 Node/pnpm 工具 image；默认优先使用 `ghcr.io/elel-code/group-11-build-ubuntu:26.04`。 |
| `image.rebuild` | 是否由工具链维护者从 `hbuilderx.source_dir` 重建 HBuilderX image；普通成员保持 `false`。 |
| `dependency.mall_frozen_lockfile` | 为 `true` 时要求 `package.json` 与 `pnpm-lock.yaml` 完全一致。 |
| `cache.mall_pnpm_store_volume` | Mall 专用 pnpm 包缓存卷，不与 Host pnpm store 混用。 |
| `cache.mall_pnpm_store_path` | pnpm store 在依赖容器内的绝对路径。 |
| `cache.mall_node_modules_volume` | Mall 专用 `node_modules` 卷，同时挂载给依赖容器和断网编译容器。 |
| `network.mall_dependency_mode` | 容器运行时下载依赖使用的 rootless Podman 网络模式。 |
| `network.mall_mode` | 正式 H5 编译网络；必须为 `none`。 |
| `network.use_host_proxy` | 是否把显式 Host 代理环境传入依赖容器；默认 `false`。 |

| 字段 | 作用 |
|---|---|
| `image.base/hbuilderx/rebuild` | Ubuntu 基础镜像、无图形 HBuilderX 镜像名和重建开关 |
| `hbuilderx.source_dir` | 仅重建镜像时读取的宿主 HBuilderX 目录 |
| `build.platform` | 当前固定 `h5` |
| `build.clean_output` | 构建前是否清空 H5 输出 |
| `media.legacy_origins` | H5 退休远程媒体源 |
| `media.legacy_fallback` | 无法代理媒体的本地回退资源 |
| `network.mall_mode` | H5 构建容器网络；可复现构建固定 `none` |
| `runtime.mall_memory/mall_cpus` | H5 构建容器资源上限 |

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

## CRM 客户画像临时数据验收配置

`tests/acceptance/verify-crm-customer-portrait-runtime.sh` 会写入并自动清理唯一前缀临时客户。账号和密码必须放在
ignored 的 `verify-*-local.yaml`，仓库只提供 `.example.yaml` 字段模板。

| 字段 | 作用 |
|---|---|
| `endpoint.base_url/tenant_id` | 管理端 API 根地址和验收租户 |
| `account.username/password` | 调用客户画像 API 的管理端账号 |
| `mysql.container/user/password/database` | 构造和清理临时数据的运行库连接 |
| `acceptance.dept_id/owner_user_id` | 客户画像查询部门和临时客户负责人 |
| `acceptance.area_id` | 有效的最细地区编号，用于省份聚合与区域钻取 |
| `acceptance.name_prefix` | 临时客户唯一前缀，只允许字母、数字、下划线和连字符 |
| `acceptance.start_time/end_time` | 与临时客户创建时间隔离的验收时间窗口 |

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

## CRM 性能与容量基线配置

| 字段 | 作用 |
|---|---|
| `endpoint.base_url/tenant_id` | CRM 管理 API 地址和真实验收租户 |
| `account.username/password` | 只读负载使用的登录账号；真实密码只在 ignored 本机 YAML |
| `workload.warmup_requests` | 每个场景正式采样前的预热次数 |
| `workload.requests_per_scenario` | 每个场景正式计量的请求数 |
| `workload.concurrency` | 正式采样并发数，不得大于场景请求数 |
| `workload.timeout_seconds` | 单请求超时秒数 |
| `workload.page_size` | 客户和商机列表页大小 |
| `workload.department_id/interval` | 统计部门和时间粒度 |
| `workload.start_time/end_time` | 统计场景的固定数据时间窗 |
| `thresholds.max_error_rate_percent` | HTTP 或业务码失败的最大百分比 |
| `thresholds.max_p95_ms/max_p99_ms` | 延迟分位数上限，单位毫秒 |
| `thresholds.min_throughput_rps` | 每个场景最低吞吐，单位请求/秒 |
| `containers.server/mysql/redis` | 采集资源与就绪状态的容器名 |
| `mysql.user/password/database` | 只读统计数据库表规模所需连接信息 |
| `evidence.output_dir` | Markdown 和 TSV 证据输出目录，相对 YAML 解析 |

## CRM 可观测诊断配置

| 字段 | 作用 |
|---|---|
| `deployment.pod_name` | 需要诊断的 rootless Pod 名称 |
| `endpoint.server_health_url/web_url/mall_url` | 三个对外健康入口 |
| `endpoint.timeout_seconds/expected_health_status` | HTTP 超时与 Server 期望健康状态 |
| `collection.log_since` | 最近日志窗口，如 `30m`、`2h` |
| `collection.output_dir` | ignored 原始诊断目录，相对 YAML 解析 |
| `containers.*` | MySQL、Redis、RabbitMQ、TDengine、Server、Web、Mall 容器名 |
| `mysql.user/password/database` | 只读采集连接数、表数与库大小的账号和库名 |
| `thresholds.min_disk_free_percent` | 项目所在文件系统最低可用百分比 |
| `thresholds.max_host_memory_used_percent` | 主机最大内存使用百分比 |
| `thresholds.max_mysql_connection_used_percent` | MySQL 已用连接最大百分比 |
| `thresholds.max_recent_error_lines` | 日志窗口内 ERROR/FATAL 最大行数 |
| `thresholds.max_container_restarts` | 单容器允许的最大重启次数 |
