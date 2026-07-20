# Mitedtsm 离线部署包

本目录包含目标机运行所需的 8 个 Docker-compatible 镜像归档、数据库 SQL、BPM 配置以及
Podman/Docker Compose 一键脚本。目标机不需要项目源码、JDK、Maven、Deno、Node.js 或 HBuilderX。

## 容器引擎自动选择

首次部署按以下顺序检测：

1. 当前普通用户可实际使用 rootless Podman 时选择 Podman；
2. 否则，当前用户可访问 Docker daemon 且 `docker compose` 可用时选择 Docker Compose；
3. 两者都不可用则停止并给出错误，不会静默跳过检查。

首次部署成功选定的引擎写入 `.container-engine`。后续 `start.sh`、`stop.sh`、`verify.sh` 始终使用同一
引擎，避免误操作另一套同名卷。Docker Compose 中 Redis、RabbitMQ、TDengine、Server、Web 和 Mall
都加入 MySQL 容器的网络命名空间，由 MySQL 统一发布 8080–8082，因此与 Podman Pod 一样可继续通过
`127.0.0.1` 互相访问。

## 目标机要求

- Linux x86-64；
- rootless Podman，或 Docker Engine + Docker Compose v2；
- `bash`、`curl`、`jq`、`sha256sum`、`od` 和 `tr`；
- 8080、8081、8082 未被占用；
- 解压后的目录支持软链接和可执行文件。

## 生产数据与凭据（默认）

`./configure.sh` 首次运行会创建权限为 `600` 的 `deployment-config.kdl`，并为 MySQL root、MySQL
应用账号、Redis、RabbitMQ 和 TDengine root 分别生成随机密码。随后从无凭据的
`podman/config/runtime-template.kdl` 生成三份实际运行 KDL。离线包不会携带构建机的这些基础设施密码。

如需指定凭据，应在首次部署前执行：

```bash
./configure.sh --host 192.168.1.20 --profile production
${EDITOR:-vi} deployment-config.kdl
./configure.sh
```

密码允许 8–128 位 `A-Z a-z 0-9 . _ ~ -`。MySQL 应用用户名可单独设置；TDengine 初始化当前管理
`root` 用户，所以用户名保持 `root`，密码可以修改。数据卷建立后不能只改 KDL 来轮换密码：应先在对应
服务内完成密码轮换，再同步 KDL，否则旧卷会拒绝新凭据。

数据配置默认是 `production`：

- 全新 MySQL 卷完成基础建表后执行 `database/datasets/none.manifest`，清除上游自带的示例 CRM 数据；
- 对已有数据库使用 `preserve`，不会替换已有业务数据；
- CRM 演示数据文件仍保留在包中，但不会自动导入。

只有明确用于演示环境时才启用：

```bash
./configure.sh --demo-data
```

`demo` 使用显式 `replace` 数据集策略，可能替换对应演示域数据，禁止在生产库启用。
要显式恢复生产策略可运行 `./configure.sh --no-demo-data`；首次配置和 `deploy.sh` 默认已经是该模式。

## 首次部署

最简生产部署只需：

```bash
cd mitedtsm-offline-x86_64
./deploy.sh 192.168.1.20
```

参数是其他浏览器访问该主机时使用的 IP 或 DNS 名。省略时公开 URL 保持 `127.0.0.1`。脚本会依次
生成/应用目标机配置、检测引擎、校验 8 个归档 SHA-256、执行无状态预检、加载镜像并部署。

访问地址：

- Server：`http://目标机:8080/actuator/health`
- Web：`http://目标机:8081/`
- Mall：`http://目标机:8082/`

## 日常操作

```bash
./verify.sh       # 检查 7 个常驻服务和三个 HTTP 端点
./stop.sh         # 停止 Pod/Compose 服务，保留定义和四个数据卷
./start.sh        # 关机或 stop.sh 后快速启动，不重灌数据
```

`deploy.sh` 用于首次部署或明确更新；已有业务环境日常重启应使用 `start.sh`。这些脚本不会删除持久数据卷。
默认监听 `0.0.0.0:8080-8082`，目标机仍须通过防火墙、安全组或反向代理限制可信访问来源。基础 SQL 中的
应用管理员初始账号属于应用初始化契约，完成 BPM provision 后应立即按生产制度修改其密码。
