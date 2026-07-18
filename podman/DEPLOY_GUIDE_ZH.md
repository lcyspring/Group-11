# Mitedtsm Rootless Podman 全流程指南

> 默认运行模板将 8080、8081、8082 绑定到 `0.0.0.0`，并允许任意 CORS 来源。该默认值方便团队成员从
> 其他 Host 验收，但部署者必须通过 UFW/安全组限制访问网段。Bearer Token 登录不依赖 credentialed CORS。

返回：[Podman 中文 README](README_ZH.md)。

## 1. 统一原则

项目使用 rootless Podman，不使用项目原有 Docker、Docker Compose 或
Docker socket。Server、InitService、CRM 测试/JaCoCo 和管理端 Web 均在
专用 Ubuntu 26.04 容器中编译。

构建和运行入口的命令行都只接受一个 KDL 配置文件路径：

```bash
cd /path/to/Group-11/podman
bash ./compile.sh ./config/build-ubuntu-26.04.kdl
bash ./build-images.sh ./config/runtime-images-check.kdl
bash ./deploy.sh ./config/runtime-local-check.kdl
bash ./stop.sh ./config/runtime-local-check.kdl
bash ./operations/images/image-archives.sh ./config/runtime-local-check.kdl
```

零参数、多参数、旧的 `--check`/`--fast`/`--volumes` 等调用都会以退出码
2 拒绝。运行脚本也不再使用环境变量覆盖配置。

## 2. 首次准备

宿主机只需可用的 rootless Podman。确认：

```bash
podman info --format '{{.Host.Security.Rootless}}'
```

输出必须为 `true`。Podman、Pasta、`uidmap` 和 rootless 存储组件应通过宿主发行版的软件包
管理器安装；仓库不再提供会向 Host 安装 JDK、Maven、Node.js 或 pnpm 的旧脚本。项目编译工具链
全部来自公开 Ubuntu 26.04 镜像。

项目目录必须支持软链接。pnpm 直接在 `Web/` 工作，旧的不支持软链接目录
的暂存、复制和回写兼容路径已不再使用。

## 3. Ubuntu 26.04 容器编译

完整编译：

```bash
cd podman
bash ./compile.sh ./config/build-ubuntu-26.04.kdl
```

KDL 显式声明基础镜像、工具链、构建模块、测试/覆盖率开关、并发度、网络
代理策略、资源限制和缓存卷。当前工具链为 JDK 17、Maven、Node 22 与
pnpm；具体版本以 `Containerfile.build-ubuntu` 和 KDL 为准。

输出包括：

- `Server/mitedtsm-server/target/mitedtsm-server.jar`
- `InitService/target/mitedtsm-init-service.jar`
- `Web/dist-prod/`
- CRM Surefire 与 JaCoCo 报告

Mall H5 构建产物位于被 Git 忽略的 `MallFrontend/unpackage/dist/build/web/`，Mall 源码变化后必须
使用 Ubuntu 26.04 HBuilderX 容器重新生成，不能依赖仓库中的旧产物。

Mall 的 `pnpm install` 由 Ubuntu 26.04 依赖容器在运行时执行，依赖写入专用
Podman named volume。不要在 Host 执行 Mall 的依赖安装；HBuilderX 编译容器挂载该卷并
使用 `--network=none`，只把最终 `unpackage` 构建产物写回工作区。

## 4. 封装运行镜像

编译成功后单独执行镜像封装；该阶段只读取 JAR、`dist-prod` 和 H5 产物，不运行编译器，
也不会启动或替换容器：

```bash
bash ./build-images.sh ./config/runtime-images-check.kdl
bash ./build-images.sh ./config/runtime-images.example.kdl
```

`operation.mode` 为 `check` 或 `package`，`build.targets` 为 `all`，或
`init-service,server,web,mall` 的逗号分隔子集。数据库不属于封装目标，运行时直接使用 KDL 的
`image.mysql_base` 官方镜像。基础镜像来源、归档名、产出镜像名及代理策略
全部由该 KDL 显式声明。

## 5. 运行配置

`config/runtime-local-check.kdl` 是安全模板，其启动和停止模式都是
`check`。建议复制成不提交的本机配置，然后只修改 KDL：

```bash
cp ./config/runtime-local-check.kdl ./config/runtime-local.kdl
```

主要分组如下：

- `operation`：启动、停止、镜像归档模式及停止时是否删除数据卷；
- `deployment`：Pod 名和优雅停止超时；
- `network`：宿主/容器端口、绑定地址和显式代理 URL；
- `image` / `archive`：基础镜像、运行镜像、来源和离线归档；
- `container` / `volume`：全部容器名和持久卷名；
- `mysql` / `rabbitmq` / `tdengine`：服务连接及凭据；
- `server`：Spring profile；
- `security`：Mock、BCrypt、XSS、CORS、文档、Druid、Actuator 等安全策略；
- `integration`：微信、社交登录、地图、支付回调和快递 Provider；
- `file`：文件存储模式、主配置和公开基地址；
- `health`：探测路径、间隔、重试次数和数据库校验 SQL。

所有字段的作用、取值和安全边界见 `config/KDL_FIELDS_ZH.md`。

相对路径以 KDL 文件所在目录为基准。解析器不执行配置内容，只接受顶层
键和一层子映射；缺值、重复键、超过两层、非法布尔值/端口/模式都会失败。

注意：示例凭据仅适用于本地开发。面向共享或生产环境时必须在不提交的 KDL
中更换；不要把真实秘密提交到 Git。

## 6. 无状态预检

保持两个模式为 `check`，依次运行：

```bash
bash ./deploy.sh ./config/runtime-local-check.kdl
bash ./stop.sh ./config/runtime-local-check.kdl
bash ./tests/runtime-config/run.sh ./config/runtime-local-check.kdl
```

预检不会拉取/加载/构建镜像，不会创建、启动、停止或删除 Pod/卷。镜像封装预检和部署预检是
两个独立配置入口。结构化测试
还会验证 KDL 标量解析、重复键拒绝、层级限制、单参数契约以及 Pod 状态前后
一致。

## 7. 启动和替换

在本机配置中设置 `operation.startup_mode`，再始终执行同一命令：

```bash
bash ./deploy.sh ./config/runtime-local.kdl
```

模式说明：

- `replace`：加载/拉取已封装运行镜像，保留数据卷并替换 Pod；
- `fast`：启动已有的停止状态 Pod，并补齐缺失前端容器；
- `frontends-only`：仅替换运行中 Pod 的 Web 和 Mall 容器；
- `replace-server`：执行兼容迁移并只替换预先封装的 Server 镜像；
- `replace-web`：只替换预先封装的 Web 镜像；
- `replace-mall`：只替换预先封装的 Mall 镜像；
- `check`：只预检。

`deploy.sh` 不检查 Host 源码产物，也不执行 `podman build`。后端、管理端或商城端变化时，必须先用
阶段一生成产物、阶段二封装相应镜像，再分别选择 `replace-server`、`replace-web` 或 `replace-mall`。

`replace` 和 `replace-server` 会调用部署期数据库 provision：SQL 始终留在仓库 `database/`，通过
stdin 发送给官方 MySQL 容器，不复制进镜像，也不挂载到长期运行容器。`mysql.bootstrap_policy` 为
`initialize-empty` 时只初始化已确认的空库；已有 `system_users` 标记的库默认保留数据并仅重放幂等
兼容清单；非空但无法识别的库直接拒绝。`require-existing` 则拒绝初始化空库。

数据生成不属于部署：独立生成器只产出 SQL/manifest/checksum，不连接数据库。部署若要消费已生成
数据集，在运行 KDL 显式配置 `dataset_manifest` 和唯一的 `mysql.dataset_mode`：`preserve` 不修改已有
数据，`insert` 只执行不含 cleanup 的插入清单，`replace` 强制清单第一项为 cleanup，并严格按“清理
旧数据集→插入新数据集”的顺序执行。不再使用多个确认布尔值表达同一个数据集意图。

## 8. 停止与数据删除

仓库提供两份不含凭据、不可忽略的显式示例。日常停服直接使用保留数据的示例：

```bash
bash ./stop.sh ./config/cleanup-stop.example.kdl
```

它会删除运行 Pod，但保留 MySQL、Redis、RabbitMQ、TDengine 四个 named volume。构建产物也不会被
删除。

只有灾难恢复演练、演示数据全量替换或明确要求建立全新环境时，才复制销毁示例：

```bash
cp ./config/cleanup-reset.example.kdl ./config/runtime-reset-local.kdl
bash ./operations/database/database-backup.sh ./config/database-backup-local.kdl
bash ./stop.sh ./config/runtime-reset-local.kdl
```

`cleanup-reset.example.kdl` 显式同时设置 `remove_volumes_on_down: true` 和
`confirm_persistent_data_reset: true`，会永久删除四个数据卷；缺少任一确认时脚本拒绝执行。
`runtime-reset-local.kdl` 被 Git 忽略，用于记录操作者核对后的本机卷名。该操作没有命令行快捷开关。
重建后先用阶段二封装全量运行镜像，再使用 `deploy.sh replace`，并确保 `bpm.provision_after_start: true`，否则空数据库中不存在 Flowable
流程定义，请假、回款、报销、合同、退款、出差、借款、客户拜访和请示提交审批都会失败。标准聚合清单必须包含
`bpm-provision-leave-local.kdl`；已有环境若仅缺少请假模型，执行
`bash ./operations/bpm/provision-bpm-model.sh ./config/bpm-provision-leave-local.kdl` 幂等补配，不需要重建数据库卷。

清理构建产物与销毁数据是两类操作：Server 的 Maven `target`、`Web/dist-prod` 和 Mall
`unpackage/dist` 由各 Ubuntu 26.04 构建 KDL 的 clean 开关控制；不要通过删数据卷来解决旧前端或
旧 JAR 问题。

## 9. 离线镜像与代理

联网机器在 KDL 中设置 `operation.archive_mode: pull-save` 后调用
`operations/images/image-archives.sh`；已有本地镜像时可使用 `save`，无状态检查使用 `check`。
再为部署配置：

```kdl
image {
  source "archive"
  archive_dir "../images"
}
```

`image.source` 支持 `auto`、`archive`、`pull`。归档文件名也全部在
`archive` 分组中显式声明。

运行期默认 `network.use_host_proxy: false`，脚本会清空代理环境并给 Podman
传入 `--http-proxy=false`。启用时必须在 KDL 中至少写一个明确 URL：

```kdl
network {
  use_host_proxy #true
  http_proxy "http://127.0.0.1:7890"
  https_proxy "http://127.0.0.1:7890"
  all_proxy "none"
}
```

脚本不会读取宿主代理环境作为隐式配置；配置 URL 中的 loopback 会转换为
`network.host_proxy_name`。

## 10. 常见问题

| 现象 | 处理 |
|---|---|
| 命令返回退出码 2 | 只传一个 KDL 路径，并检查缺值、重复键、端口和模式。 |
| `Run this script as the rootless Podman user` | 不要 sudo，修复当前用户的 Podman 会话。 |
| 缺少 JAR 或 `Web/dist-prod` | 先运行 Ubuntu 26.04 构建配置。 |
| 离线模式报告缺归档 | 核对 `image.archive_dir` 和 `archive.*` 文件名。 |
| 页面打开但 API 失败 | 检查 Web 产物使用相对 `/admin-api`，并核对 Server 健康路径。 |
| Pod 已停止但镜像未变 | 使用 `startup_mode: fast`。 |
| Pod 已删除但镜像仍有效 | 使用 `startup_mode: replace`。 |
| 只更新管理端 | 先用 `build.targets: web` 封装，再使用 `startup_mode: replace-web`。 |
| 只更新商城端 | 先容器编译 H5、用 `build.targets: mall` 封装，再使用 `startup_mode: replace-mall`。 |

## 11. 安全检查清单

- 当前用户的 Podman 为 rootless；
- 编译来自 Ubuntu 26.04 专用镜像；
- 运行命令只传 KDL 路径；
- 真实凭据只在不提交的本机配置中；
- 首次执行状态操作前先运行 `check` 和结构化测试；
- 删除卷前再次核对 `remove_volumes_on_down: true` 与 `confirm_persistent_data_reset: true`；
- 不使用项目原 Docker 文件和 Compose。
