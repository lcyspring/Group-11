# Rootless Podman 中文说明

本项目使用 rootless Podman，不使用项目原有 Docker、Docker Compose 或 Docker socket。编译、测试、
镜像打包、部署、备份和恢复入口的命令行都只接受一个 KDL 配置路径。

## 编译镜像强制约定

所有成员日常编译、测试必须直接使用 `elel-code` 命名空间下已经公开的两个工具链镜像：

- Server、InitService、Web、后端测试：`ghcr.io/elel-code/group-11-build-ubuntu:26.04`；
- Mall H5：`ghcr.io/elel-code/group-11-hbuilderx-ubuntu:26.04-5.05`。

普通成员不需要、也不应先在本机重新构建工具链镜像。共享 KDL 必须保持 `image.rebuild: false`，脚本
直接 pull 或使用本地缓存的上述 public image。只有维护者发布新的工具链版本时，才允许在专用维护配置
中显式设置 `image.rebuild: true`。无网络环境使用这两个镜像对应的 OCI tar，不能改用 Host JDK、Node、
pnpm、HBuilderX，也不能改用项目原有 Docker/Compose 构建链。

## 快速导航

- [编译、构建与部署操作手册](OPERATIONS_ZH.md)
- [完整部署指南](DEPLOY_GUIDE_ZH.md)
- [KDL 全字段参考](config/KDL_FIELDS_ZH.md)
- [配置文件分类](config/README_ZH.md)
- [镜像归档说明](images/README_ZH.md)
- [各镜像来源、作用与归档策略](images/README_ZH.md#镜像来源与作用)

## 日常命令

首次使用直接下载固定版本的官方 KDL 解析器。脚本固定 dasel `v3.11.2` GitHub Release，并分别校验
Linux amd64/arm64 官方资产的 SHA-256；不需要安装 Go 或 git。产物只写入被 Git 忽略的
`podman/tools/bin/dasel`，各入口不会调用系统全局 dasel：

```bash
bash ./podman/tools/install-dasel.sh
./podman/tools/bin/dasel version
```

```bash
cd /path/to/Group-11

# Server、InitService、管理端、CRM 测试和 JaCoCo
bash ./podman/compile.sh ./podman/config/build-ubuntu-26.04.kdl

# Mall H5 无图形构建
bash ./podman/compile.sh ./podman/config/build-mall-h5-ubuntu-26.04.kdl

# 一次选择四个产物；白名单/黑名单均在 KDL 中显式配置
bash ./podman/compile.sh ./podman/config/compile-all-ubuntu-26.04.example.kdl

# 阶段二：检查并封装已有产物为运行镜像
bash ./podman/build-images.sh ./podman/config/runtime-images-check.kdl
bash ./podman/build-images.sh ./podman/config/runtime-images.example.kdl

# 阶段三：部署配置无状态预检
bash ./podman/tests/runtime-config/run.sh ./podman/config/runtime-local-check.kdl

# 部署；具体模式只在 KDL 中修改
bash ./podman/deploy.sh ./podman/config/runtime-local.kdl
```

完整部署会先优雅停止旧服务，再使用 `podman pod create --replace` 接管同名 Pod；基础设施、Server、
Web、Mall 及单组件更新均使用 `podman run --replace`。只有 `stop.sh` 的显式停服/销毁操作保留
`pod rm`，避免部署脚本手工维护重复删除分支。

## 产物与镜像

- 后端产物：`Server/mitedtsm-server/target/mitedtsm-server.jar`；
- 管理端产物：`Web/dist-prod/`，进入独立 Nginx 镜像；
- Mall H5：`MallFrontend/unpackage/dist/build/web/`，本地生成且 Git 忽略，进入独立 Nginx 镜像；
- Mall 项目依赖由 Ubuntu 26.04 容器运行时安装到 Podman named volume，Host
  `node_modules` 不参与；依赖阶段可联网，HBuilderX 编译阶段固定断网；
- 前端不会打进后端 JAR/WAR；
- Ubuntu Server/Web 与 HBuilderX 编译工具链镜像必须使用上文指定的 `ghcr.io/elel-code` 公共镜像；
- `ghcr.io/elel-code` 下两个编译工具链镜像当前为 public，pull 无需登录，push 仍需维护者登录；
- Server/Web/Mall 项目运行镜像只能在编译成功后由 `build-images.sh` 独立封装；`deploy.sh`
  不读取源码产物，也不执行镜像构建。
- MySQL 直接使用 KDL 中固定到 `8.0.46 + sha256 digest` 的官方镜像；`database/` SQL 留在仓库，由 `deploy.sh` 在部署期
  通过 stdin 初始化确认空库或重放幂等兼容清单，不烘焙到运行镜像。

## 配置分类

- 日常三阶段：编译使用 `build-*.kdl`，运行镜像封装使用 `runtime-images*.kdl`，启动/替换使用
  `runtime-local-check.kdl` 和被忽略的 `runtime-local*.kdl`；
- 专项测试：`verify-*`、`test-*`、`check-*`，用于复现功能验收，不是普通启动配置；
- 数据保护：`database-backup-check.kdl` 复制为 ignored 本机配置后执行；
- 编译镜像：`build-image-archives-check.kdl` 复制为 ignored 本机配置后选择 save/load/push。
- 性能验收：`verify-crm-performance-baseline.example.kdl` 复制为 ignored 本机配置后执行只读负载。
- 可观测诊断：`crm-diagnostics.example.kdl` 复制为 ignored 本机配置，原始包写入 `podman/diagnostics/`。

## 安全边界

- `runtime-local-check.kdl` 不改变 Pod/卷；
- `network.admin_ui_public_url` 必须改为用户实际可访问的管理端地址，审批短信链接不使用代码内固定 IP；
- BPM 短信默认关闭；启用前必须同时配置短信 Provider 与
  `bpm-notification-templates-local.kdl` 中四个供应商模板 ID；
- 删除数据卷必须在 KDL 同时选择 stop、`remove_volumes_on_down: true` 和
  `confirm_persistent_data_reset: true`；
- 恢复覆盖真源库必须二次授权且 Server 已停止；
- 真实密码只写被 Git 忽略的本机 KDL；
- 编译镜像 tar、备份文件和 checksum 均被 Git 忽略，不提交仓库。

## 脚本目录

- 根目录只保留 `compile.sh`、`build-images.sh`、`deploy.sh`、`stop.sh` 四个日常入口；
- `tests/acceptance/` 保存真实 API/MySQL 验收脚本，`tests/*` 不参与普通启动；
- `operations/database|images|bpm|diagnostics/` 保存低频运维入口；
- `internal/` 保存容器内入口和标准编译助手，成员不应直接调用。
- `tools/install-dasel.sh` 下载并校验固定官方 Release；`tools/bin/` 是本机可复现产物，不提交 Git。
