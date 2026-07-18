# Group-11
# Mitedtsm CRM 系统

本仓库在 `develop` 分支持续完善 CRM、财务、营销与流程协作能力。项目统一使用 rootless
Podman；JDK、Maven、Node.js、pnpm 与 HBuilderX 均在 Ubuntu 26.04 工具链容器中运行，宿主机
不安装项目编译工具链，也不在宿主机下载项目依赖。

## 目录

| 目录 | 作用 |
|---|---|
| `Server/` | Java 后端与模块测试 |
| `Web/` | 管理端前端 |
| `MallFrontend/` | 商城 H5 前端 |
| `InitService/` | 初始化服务 |
| `database/` | 建表迁移、可选数据集、维护和销毁脚本 |
| `podman/` | YAML 驱动的编译、镜像封装、部署与运维入口 |
| `docs/20-CRM-Delivery/` | 当前开发的功能、测试、覆盖率、Bug 和交付文档 |

## 环境要求

宿主机只需 Git、rootless Podman 及其网络组件。公开工具链镜像为：

- `ghcr.io/elel-code/group-11-build-ubuntu:26.04`：Server、InitService、Web、测试和覆盖率；
- `ghcr.io/elel-code/group-11-hbuilderx-ubuntu:26.04-5.05`：Mall H5 无图形化编译。

项目依赖在编译容器运行时下载到 Podman 命名卷，不写入宿主机 `node_modules`，也不在构建
工具链镜像时下载项目依赖。

## 三阶段交付

所有入口只接受一个 YAML 配置文件路径。配置字段、示例和运行模式详见
[`podman/README_ZH.md`](podman/README_ZH.md) 与
[`podman/config/YAML_FIELDS_ZH.md`](podman/config/YAML_FIELDS_ZH.md)。

```bash
# 1. 编译 Server、InitService、Web 和测试
bash podman/compile.sh podman/config/build-ubuntu-26.04.yaml

# 也可用 HBuilderX 工具链编译 Mall H5
bash podman/compile.sh podman/config/build-mall-h5-ubuntu-26.04.yaml

# 一次选择四个目标
bash podman/compile.sh podman/config/compile-all-ubuntu-26.04.example.yaml

# 2. 将已有产物封装为运行镜像
bash podman/build-images.sh podman/config/runtime-images.example.yaml

# 3. 只消费镜像并启动或替换容器
bash podman/deploy.sh podman/config/runtime-local.yaml
```

`build.include_targets` 是白名单，`build.exclude_targets` 是优先级更高的黑名单；可用 `all`、
`none` 或 `server,init-service,web,mall-h5` 子集组合，不再使用每个产物一个布尔开关。

停止服务：

```bash
bash podman/stop.sh podman/config/cleanup-stop.example.yaml
```

默认本地入口为：

- 后端健康检查：`http://127.0.0.1:8080/actuator/health`
- 管理端：`http://127.0.0.1:8081/`
- 商城 H5：`http://127.0.0.1:8082/`

如需供局域网其他主机访问，应在运行 YAML 中显式设置 `network.host_address: 0.0.0.0`，并按
实际访问源配置 CORS；不要依赖脚本硬编码或宿主环境变量。

## 数据库与演示数据

`deploy.sh` 根据运行 YAML 选择 `mysql.dataset`、创建新卷并执行 bootstrap，已有卷则执行幂等
compatibility migrations。演示数据是否加载、替换时是否清理旧数据必须由数据集 YAML/manifest
显式决定；生产环境不要选择演示数据集。

数据库脚本按职责位于：

- `database/schema/`：基础建表；
- `database/migrations/`：幂等增量迁移；
- `database/seed/` 与 `database/datasets/`：可选择的初始化/演示数据；
- `database/maintenance/`：清理与维护；
- `database/destruction/`：显式销毁。

## 开发约定

- 功能实现、测试结果、覆盖率和 Bug 修复分别放入 `docs/20-CRM-Delivery/` 的独立目录；
- 数据库变更必须登记到相应 manifest，并保证重复执行安全；
- 配置进入 YAML，命令行只指定 YAML 路径；账号和密钥只放在被忽略的本机 YAML；
- 提交说明使用 `Where : feat/change description`，例如
  `CRM Marketing : feat add secure recipient link click analytics`；
- 当前工作只在本地 `develop` 分支提交，除非明确要求，否则不推送远端。

完整构建、部署、备份、恢复和故障排查步骤见
[`podman/DEPLOY_GUIDE_ZH.md`](podman/DEPLOY_GUIDE_ZH.md)。
