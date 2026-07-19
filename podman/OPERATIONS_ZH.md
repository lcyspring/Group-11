# 编译、构建与部署操作手册

返回：[Podman 中文 README](README_ZH.md)。

## 开始前必须确认

日常编译不是“可选地优先”使用公共镜像，而是统一使用以下固定工具链：

```text
Server / InitService / Web / 测试：ghcr.io/elel-code/group-11-build-ubuntu:26.04-deno-2.9.3
Mall H5：                         ghcr.io/elel-code/group-11-hbuilderx-ubuntu:26.04-5.05
```

普通成员直接运行下方 KDL 命令即可，首次运行由 Podman 拉取 public image，不需要先执行镜像构建，
也不需要在 Host 安装 JDK、Deno、Node、pnpm 或 HBuilderX。日常配置必须使用 `image.rebuild: false`；
`image.rebuild: true` 仅用于维护者发布新版工具链镜像，不属于项目编译步骤。

## 职责边界

| 阶段 | 命令 | 输出/作用 |
|---|---|---|
| Server/Web/测试编译 | `compile.sh <kdl>` | JAR、Web `dist-prod`、Deno LCOV 与 JaCoCo |
| Mall H5 编译 | `compile.sh <kdl>` | 本地 ignored 的 H5 构建目录 |
| 运行镜像封装 | `build-images.sh <kdl>` | 只把已有产物封装为 Init/Server/Web/Mall 四个应用镜像 |
| 启动/替换容器 | `deploy.sh <kdl>` | 只消费运行镜像，启动或替换 rootless Pod/容器 |
| 停止 | `stop.sh <kdl>` | 停止 Pod；是否删卷只看 KDL |
| 离线镜像 | `operations/images/image-archives.sh <kdl>` | 保存/拉取并保存基础镜像 tar |
| 编译镜像归档/上传 | `operations/images/build-image-archives.sh <kdl>` | 两个工具链镜像 check/save/load/push |
| CRM 数据备份 | `operations/database/database-backup.sh <kdl>` | MySQL 一致性压缩备份和 SHA-256 |
| CRM 恢复演练 | `operations/database/database-restore.sh <kdl>` | 隔离库恢复、核心表检查和可选清理 |
| CRM 性能基线 | `tests/acceptance/verify-crm-performance-baseline.sh <kdl>` | 五类只读负载、分位数、错误率和吞吐证据 |
| CRM 诊断包 | `operations/diagnostics/collect-crm-diagnostics.sh <kdl>` | 健康、容器、日志、数据库与宿主 SLI 诊断 |
| 配置门禁 | `tests/runtime-config/run.sh <kdl>` | 无状态检查 KDL、manifest、脚本和 Pod 不变性 |
| 数据库部署期 provision | `deploy.sh <kdl>` | 直接运行官方 MySQL，并通过 stdin 初始化空库或执行幂等兼容迁移 |

宿主 JDK/Deno/Node/pnpm 构建入口已删除，所有成员统一通过 `compile.sh` 使用上述 `elel-code`
公共镜像。项目原 Docker/Compose 不进入本流程。

## 标准流程

```bash
cd /path/to/Group-11
bash ./podman/compile.sh ./podman/config/build-ubuntu-26.04.kdl
bash ./podman/compile.sh ./podman/config/build-mall-h5-ubuntu-26.04.kdl
# 或使用 include_targets/exclude_targets 一次选择四个编译目标
bash ./podman/compile.sh ./podman/config/compile-all-ubuntu-26.04.example.kdl
bash ./podman/build-images.sh ./podman/config/runtime-images.example.kdl
bash ./podman/tests/runtime-config/run.sh ./podman/config/runtime-local-check.kdl
bash ./podman/deploy.sh ./podman/config/runtime-local.kdl
```

全新数据库卷会同时清空 Flowable 已部署定义。`runtime-local.kdl` 应显式设置
`bpm.provision_after_start: true` 和 `bpm.provision_manifest: bpm-provision-all-local.kdl`；`deploy.sh replace`
在 Server 健康后、暴露前端前自动恢复请假、回款、报销、合同、退款、出差、借款、客户拜访和请示流程。若任一模型失败，部署
立即失败，不会继续显示一个看似可用但提交审批必然报错的前端。

## 停服、清产物与数据重置

三种操作必须分开选择：

| 目标 | 配置/入口 | 结果 |
|---|---|---|
| 日常停服 | `stop.sh config/cleanup-stop.example.kdl` | 删除 Pod，保留四个持久卷和全部构建产物 |
| 重新编译 | 对应 Ubuntu 26.04 构建 KDL 的 `build.clean: true` | 清理并重建所选源码产物，不触碰业务数据 |
| 全新数据环境 | 复制 `cleanup-reset.example.kdl` 为 ignored `runtime-reset-local.kdl` 后执行 `stop.sh` | 永久删除 MySQL、Redis、RabbitMQ、TDengine 卷 |

执行全新数据环境前必须先备份并核对 KDL 中的四个卷名。之后先封装运行镜像，再使用 `deploy.sh replace` 重建；运行 KDL
必须开启 BPM 自动恢复。`stop.sh` 会逐个打印被保留或被删除的卷，不再静默完成数据销毁。

所有 `.example.kdl` 都属于仓库交付内容，不得加入 `.gitignore`；真实账号、密码和环境地址写入
对应 ignored `*-local.kdl`。BPM 各单模型示例、聚合清单和字段含义见
`config/README_ZH.md` 与 `config/KDL_FIELDS_ZH.md`。

管理端和 Mall 产物分别进入独立 Nginx 镜像，后端只打成可执行 JAR，前端不会塞入 JAR/WAR。

## 按变更选择模式

| 变更 | 阶段一编译 | 阶段二 `build.targets` | 阶段三 `startup_mode` |
|---|---|---|---|
| Java/配置 | Ubuntu Server build | `server` | `replace-server` |
| 数据库建表/迁移/数据集 | 无编译；维护 `database/` manifest | 无；数据库不封装镜像 | `replace` |
| 管理端 Web | Ubuntu Web build | `web` | `replace-web` |
| Mall H5 | Ubuntu HBuilderX build | `mall` | `replace-mall` |
| Web + Mall | 两项前端 build | `web,mall` | `frontends-only` |
| 仅重启已有 Pod | 无 | 无 | `fast` |
| 首次或完整替换 | 全部所需产物 | `all` | `replace` |

`mysql.bootstrap_policy: initialize-empty` 只允许初始化确认无表的库；
`mysql.bootstrap_policy: require-existing` 用于禁止部署脚本创建基线。已有完整库不会因切换
`mysql.dataset` 被覆盖。若确需在部署中替换，显式设置 `mysql.dataset_mode: replace` 与
`dataset_manifest`；manifest 第一项必须清理旧数据集，后续项插入新数据。整套持久卷销毁必须同时开启
`operation.remove_volumes_on_down` 和 `operation.confirm_persistent_data_reset`；已有库数据集替换则使用
上述集中模式。`operations/database/database-dataset.sh` 仅保留为低频维护/验证工具。

任何 stateful 操作前先用 `runtime-images-check.kdl` 和 `runtime-local-check.kdl`。`runtime-local.kdl`、各 replace 配置含本地凭据，
被 Git 忽略；共享模板不得写真实账号。

## 验收

部署成功必须同时满足：Server health `UP`、Web 200、Mall 200、兼容迁移成功、无启动 ERROR。CRM
还应执行对应专项；安全基线执行：

```bash
bash ./podman/tests/acceptance/verify-crm-runtime-security.sh ./podman/config/runtime-local.kdl
bash ./podman/tests/acceptance/verify-crm-user-guide.sh ./podman/config/runtime-local.kdl
bash ./podman/tests/acceptance/verify-crm-performance-baseline.sh ./podman/config/verify-crm-performance-baseline-local.kdl
```

完整字段说明见 `config/KDL_FIELDS_ZH.md`。
各基础镜像、项目运行镜像和编译工具链镜像的上游来源、构建关系及用途见
`images/README_ZH.md`，部署前不应仅凭相似镜像名判断来源。

## 备份与恢复

先复制 `config/database-backup-check.kdl` 为被忽略的 `database-backup-local.kdl`，填写真实本地
密码和归档文件名。备份设 `operation.mode: backup`；演练恢复设 `restore`、隔离目标库、
`allow_replace: true`、`allow_live_database_replace: false`、`drop_after_verify: true`。任何直接覆盖
运行库的操作还必须把第二道授权改为 true，不提供命令行捷径。
直接覆盖运行真源库前还必须先停止 Server；脚本检测到 Server 运行会拒绝恢复。

## 日常入口与验收资产

- 日常入口：根目录只保留 `compile.sh`、`build-images.sh`、`deploy.sh`、`stop.sh`；
- `tests/acceptance/verify-*.sh` 与 `config/verify-*`、`test-*`、`check-*` 是结构化测试资产，不用于普通启动；
- 数据库、镜像、BPM 和诊断入口分别位于 `operations/` 对应子目录；
- 编译工具链镜像推荐 save，并可在登录 GHCR 后使用 `operation.mode: push` 上传；项目运行镜像仍由
  当前源码产物重建。基础运行镜像只有离线交付才执行 `archive_mode: save/pull-save`。

性能基线不是普通启动步骤。它使用 ignored 本机 KDL 中的真实账号，先预热再并发采样，并按 KDL
阈值退出。开发机结果用于回退比较，不代表生产容量承诺。

可观测诊断使用 `crm-diagnostics-local.kdl`，正常和阈值失败都会保留本机摘要与压缩包。中文 SLI、
故障处置和诊断包脱敏规则见 `docs/20-CRM-Delivery/operations/crm-observability/`。
