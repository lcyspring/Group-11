# PODMAN-RUNTIME-BUG-009：脚本命名含混及 Host 工具链残留

日期：2026-07-18。分支：`develop`。级别：P1。状态：已关闭。

## 现象与根因

标准编译和 H5 编译分别暴露两个相似入口，镜像封装和容器生命周期仍使用历史 `up/down` 命名；成员
无法仅从入口名判断三阶段职责。仓库还保留会在 Host 安装 JDK、Maven、Node.js 和 pnpm 的旧脚本，
与“项目工具链只在 Ubuntu 26.04 容器运行”的现行约束冲突。

## 修复

- `compile.sh` 成为唯一公开编译入口；后续进一步用 include/exclude 目标集合取代引擎分派；
- 阶段二、三分别命名为 `build-images.sh`、`deploy.sh`，停服入口为 `stop.sh`；
- 删除旧编译、封装、up/down 入口，不保留软链接兼容层；
- 删除两个 Host 编译依赖安装脚本，宿主只需发行版提供的 rootless Podman 组件；
- 59 份编译/测试 YAML 统一目标选择协议，示例、中文指南和历史执行命令同步更新；
- `.idea`、成员版 README 和旧编辑说明另行清理，根目录只保留一份当前中文 README。

## 防回归门禁

运行配置测试会检查统一入口及两个内部引擎的 Bash 语法、单 YAML 参数契约、所有工具链 YAML 的
显式引擎、必需示例是否被 Git 跟踪，并确认部署 check 不改变 Pod 状态。真实三阶段替换后 Server
为 `UP`，Web/Mall 均返回 200。
