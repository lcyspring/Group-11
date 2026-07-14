# Ubuntu 26.04 容器构建链

更新日期：2026-07-14

## 目标

- 所有 Server、InitService、Web 构建和 CRM 自动化测试均在 Ubuntu 26.04 容器内执行。
- 使用 rootless Podman，不依赖项目原有 Docker 构建链。
- 以显式 YAML 文件保存镜像、工具链、构建步骤、缓存、网络和资源配置。
- 宿主脚本的命令行只接受一个 YAML 配置文件路径。
- Maven、pnpm store 与 Web `node_modules` 使用独立命名卷，不把宿主工具链或宿主依赖目录当作构建前提。

## 实现文件

- `podman/Containerfile.build-ubuntu`：Ubuntu 26.04、JDK 17、Maven、Node.js 与固定版本 pnpm 工具链。
- `podman/build-in-ubuntu.sh`：校验 YAML、构建镜像、建立缓存卷并启动 rootless Podman。
- `podman/ubuntu-build-entrypoint.sh`：在容器内执行 CRM 测试及 JaCoCo、Server、InitService 和 Web 构建，并验证产物。
- `podman/config/build-ubuntu-26.04.yaml`：唯一显式构建配置入口。
- `podman/config/test-crm-ubuntu-26.04.yaml`：复用相同镜像和缓存、只运行 CRM 测试与覆盖率的快速反馈配置。
- `podman/config/build-web-ubuntu-26.04.yaml`：只验证 Web frozen-lockfile 与生产构建的快速反馈配置。

## 使用方式

```bash
cd podman
bash ./build-in-ubuntu.sh ./config/build-ubuntu-26.04.yaml
```

脚本拒绝零参数或多个参数；具体构建行为不通过额外命令行开关控制，应修改 YAML 后再运行。

CRM 开发阶段的快速回归同样只传配置路径：

```bash
cd podman
bash ./build-in-ubuntu.sh ./config/test-crm-ubuntu-26.04.yaml
```

## 工具链基线

首次容器运行已确认：

- Ubuntu 26.04 LTS；
- OpenJDK 17；
- Maven 3.9.12；
- Node.js 22.22.1；
- pnpm 11.3.0（首次运行暴露版本偏差后与仓库配置对齐）。

## 已处理的构建偏差

- Podman 默认把宿主回环代理传入容器，造成容器访问代理失败：关闭代理时显式设置 `--http-proxy=false`，启用时把回环地址转换为 `host.containers.internal`。
- Ubuntu 最小镜像的默认 locale 无法表示源码树中的中文文件名：镜像显式使用 `C.UTF-8`。
- pnpm 9 无法解析仓库使用的 pnpm 11 `allowBuilds` 配置：构建工具链对齐为 pnpm 11.3.0。
- Server `clean` 删除先前生成的 CRM 测试与 JaCoCo 证据：CRM 测试调整到 Server/InitService 打包之后执行。
- 无 TTY 的容器无法确认清理宿主 `node_modules`：Web 依赖目录改用独立命名卷，并由 YAML 显式启用 CI 模式。
- pnpm 11 默认在项目所在磁盘创建 store，绕过了预期命名卷：YAML 显式声明容器 store 路径并挂载对应命名卷。

详细记录见：

- `docs/18-Review/bugs/logs/PODMAN-BUILD-BUG-001-host-loopback-proxy.md`
- `docs/18-Review/bugs/logs/PODMAN-BUILD-BUG-002-non-utf8-locale.md`
- `docs/18-Review/bugs/logs/PODMAN-BUILD-BUG-003-pnpm-workspace-version.md`
- `docs/18-Review/bugs/logs/PODMAN-BUILD-BUG-004-coverage-removed-by-clean.md`
- `docs/18-Review/bugs/logs/PODMAN-BUILD-BUG-005-host-node-modules-no-tty.md`
- `docs/18-Review/bugs/logs/PODMAN-BUILD-BUG-006-pnpm-store-mounted-at-wrong-path.md`

## 当前状态

当前 HEAD 完整构建已通过：Server、InitService、CRM 62 项测试与 JaCoCo、Web 生产资产均由 Ubuntu 26.04 容器生成。构建、测试、覆盖率和产物结果记录在 `docs/10-Testing/build/ubuntu-26.04-container-build/`。
