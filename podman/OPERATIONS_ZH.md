# 编译、构建与部署操作手册

返回：[Podman 中文 README](README_ZH.md)。

## 开始前必须确认

日常编译不是“可选地优先”使用公共镜像，而是统一使用以下固定工具链：

```text
Server / InitService / Web / 测试：ghcr.io/elel-code/group-11-build-ubuntu:26.04
Mall H5：                         ghcr.io/elel-code/group-11-hbuilderx-ubuntu:26.04-5.05
```

普通成员直接运行下方 YAML 命令即可，首次运行由 Podman 拉取 public image，不需要先执行镜像构建，
也不需要在 Host 安装 JDK、Node、pnpm 或 HBuilderX。日常配置必须使用 `image.rebuild: false`；
`image.rebuild: true` 仅用于维护者发布新版工具链镜像，不属于项目编译步骤。

## 职责边界

| 阶段 | 命令 | 输出/作用 |
|---|---|---|
| Server/Web/测试编译 | `build-in-ubuntu.sh <yaml>` | JAR、Web `dist-prod`、测试与 JaCoCo |
| Mall H5 编译 | `build-mall-h5-in-ubuntu.sh <yaml>` | 本地 ignored 的 H5 构建目录 |
| 运行镜像打包与部署 | `up.sh <yaml>` | MySQL/Init/Server/Web/Mall 镜像和 rootless Pod |
| 停止 | `down.sh <yaml>` | 停止 Pod；是否删卷只看 YAML |
| 离线镜像 | `image-archives.sh <yaml>` | 保存/拉取并保存基础镜像 tar |
| 编译镜像归档/上传 | `build-image-archives.sh <yaml>` | 两个工具链镜像 check/save/load/push |
| CRM 数据备份 | `database-backup.sh <yaml>` | MySQL 一致性压缩备份和 SHA-256 |
| CRM 恢复演练 | `database-restore.sh <yaml>` | 隔离库恢复、核心表检查和可选清理 |
| CRM 性能基线 | `verify-crm-performance-baseline.sh <yaml>` | 五类只读负载、分位数、错误率和吞吐证据 |
| CRM 诊断包 | `collect-crm-diagnostics.sh <yaml>` | 健康、容器、日志、数据库与宿主 SLI 诊断 |
| 配置门禁 | `tests/runtime-config/run.sh <yaml>` | 无状态检查 YAML、manifest、脚本和 Pod 不变性 |

宿主 JDK/Node/pnpm 构建入口已删除，所有成员统一通过 `build-in-ubuntu.sh` 使用上述 `elel-code`
公共镜像。项目原 Docker/Compose 不进入本流程。

## 标准流程

```bash
cd /path/to/Group-11
bash ./podman/build-in-ubuntu.sh ./podman/config/build-ubuntu-26.04.yaml
bash ./podman/build-mall-h5-in-ubuntu.sh ./podman/config/build-mall-h5-ubuntu-26.04.yaml
bash ./podman/tests/runtime-config/run.sh ./podman/config/runtime-local-check.yaml
bash ./podman/up.sh ./podman/config/runtime-local.yaml
```

全新数据库卷会同时清空 Flowable 已部署定义。`runtime-local.yaml` 应显式设置
`bpm.provision_after_start: true` 和 `bpm.provision_manifest: bpm-provision-all-local.yaml`；`up.sh full`
在 Server 健康后、暴露前端前自动恢复回款、报销、合同、退款、出差和借款流程。若任一模型失败，部署
立即失败，不会继续显示一个看似可用但提交审批必然报错的前端。

## 停服、清产物与数据重置

三种操作必须分开选择：

| 目标 | 配置/入口 | 结果 |
|---|---|---|
| 日常停服 | `down.sh config/cleanup-stop.example.yaml` | 删除 Pod，保留四个持久卷和全部构建产物 |
| 重新编译 | 对应 Ubuntu 26.04 构建 YAML 的 `build.clean: true` | 清理并重建所选源码产物，不触碰业务数据 |
| 全新数据环境 | 复制 `cleanup-reset.example.yaml` 为 ignored `runtime-reset-local.yaml` 后执行 `down.sh` | 永久删除 MySQL、Redis、RabbitMQ、TDengine 卷 |

执行全新数据环境前必须先备份并核对 YAML 中的四个卷名。之后使用 `up.sh full` 重建；运行 YAML
必须开启 BPM 自动恢复。`down.sh` 会逐个打印被保留或被删除的卷，不再静默完成数据销毁。

所有 `.example.yaml` 都属于仓库交付内容，不得加入 `.gitignore`；真实账号、密码和环境地址写入
对应 ignored `*-local.yaml`。BPM 各单模型示例、聚合清单和字段含义见
`config/README_ZH.md` 与 `config/YAML_FIELDS_ZH.md`。

管理端和 Mall 产物分别进入独立 Nginx 镜像，后端只打成可执行 JAR，前端不会塞入 JAR/WAR。

## 按变更选择模式

| 变更 | 构建 | `startup_mode` |
|---|---|---|
| Java/配置/数据库 | Ubuntu Server build | `rebuild-server`；数据库镜像或全链不确定时 `full` |
| 管理端 Web | Ubuntu Web build | `rebuild-web` |
| Mall H5 | Ubuntu HBuilderX build | `rebuild-mall` |
| Web + Mall | 两项前端 build | `frontends-only` |
| 仅重启现有 Pod | 无 | `fast` |
| 用现有运行镜像重建 Pod | 无 | `no-build` |
| 首次、数据库布局或基础镜像变化 | 全部所需产物 | `full` |

任何 stateful 操作前先用 `runtime-local-check.yaml`。`runtime-local.yaml`、各 rebuild 配置含本地凭据，
被 Git 忽略；共享模板不得写真实账号。

## 验收

部署成功必须同时满足：Server health `UP`、Web 200、Mall 200、兼容迁移成功、无启动 ERROR。CRM
还应执行对应专项；安全基线执行：

```bash
bash ./podman/verify-crm-runtime-security.sh ./podman/config/runtime-local.yaml
bash ./podman/verify-crm-user-guide.sh ./podman/config/runtime-local.yaml
bash ./podman/verify-crm-performance-baseline.sh ./podman/config/verify-crm-performance-baseline-local.yaml
```

完整字段说明见 `config/YAML_FIELDS_ZH.md`。
各基础镜像、项目运行镜像和编译工具链镜像的上游来源、构建关系及用途见
`images/README_ZH.md`，部署前不应仅凭相似镜像名判断来源。

## 备份与恢复

先复制 `config/database-backup-check.yaml` 为被忽略的 `database-backup-local.yaml`，填写真实本地
密码和归档文件名。备份设 `operation.mode: backup`；演练恢复设 `restore`、隔离目标库、
`allow_replace: true`、`allow_live_database_replace: false`、`drop_after_verify: true`。任何直接覆盖
运行库的操作还必须把第二道授权改为 true，不提供命令行捷径。
直接覆盖运行真源库前还必须先停止 Server；脚本检测到 Server 运行会拒绝恢复。

## 日常入口与验收资产

- 日常入口：`build-in-ubuntu.sh`、`build-mall-h5-in-ubuntu.sh`、`up.sh`、`down.sh`、
  `image-archives.sh`、`database-backup.sh`、`database-restore.sh`；
- `verify-*.sh` 与 `config/verify-*`、`test-*`、`check-*` 是结构化测试资产，不用于普通启动；
- 编译工具链镜像推荐 save，并可在登录 GHCR 后使用 `operation.mode: push` 上传；项目运行镜像仍由
  当前源码产物重建。基础运行镜像只有离线交付才执行 `archive_mode: save/pull-save`。

性能基线不是普通启动步骤。它使用 ignored 本机 YAML 中的真实账号，先预热再并发采样，并按 YAML
阈值退出。开发机结果用于回退比较，不代表生产容量承诺。

可观测诊断使用 `crm-diagnostics-local.yaml`，正常和阈值失败都会保留本机摘要与压缩包。中文 SLI、
故障处置和诊断包脱敏规则见 `docs/20-CRM-Delivery/operations/crm-observability/`。
