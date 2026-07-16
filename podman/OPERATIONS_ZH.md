# 编译、构建与部署操作手册

## 职责边界

| 阶段 | 命令 | 输出/作用 |
|---|---|---|
| Server/Web/测试编译 | `build-in-ubuntu.sh <yaml>` | JAR、Web `dist-prod`、测试与 JaCoCo |
| Mall H5 编译 | `build-mall-h5-in-ubuntu.sh <yaml>` | 本地 ignored 的 H5 构建目录 |
| 运行镜像打包与部署 | `up.sh <yaml>` | MySQL/Init/Server/Web/Mall 镜像和 rootless Pod |
| 停止 | `down.sh <yaml>` | 停止 Pod；是否删卷只看 YAML |
| 离线镜像 | `image-archives.sh <yaml>` | 保存/拉取并保存基础镜像 tar |
| 配置门禁 | `tests/runtime-config/run.sh <yaml>` | 无状态检查 YAML、manifest、脚本和 Pod 不变性 |

`build-assets.sh` 只给已经安装 JDK/Node/pnpm 的 Ubuntu 26.04 成员宿主机使用；本工作站统一使用
`build-in-ubuntu.sh`。项目原 Docker/Compose 不进入本流程。

## 标准流程

```bash
cd /path/to/Group-11
bash ./podman/build-in-ubuntu.sh ./podman/config/build-ubuntu-26.04.yaml
bash ./podman/build-mall-h5-in-ubuntu.sh ./podman/config/build-mall-h5-ubuntu-26.04.yaml
bash ./podman/tests/runtime-config/run.sh ./podman/config/runtime-local-check.yaml
bash ./podman/up.sh ./podman/config/runtime-local.yaml
```

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
```

完整字段说明见 `config/YAML_FIELDS_ZH.md`。
