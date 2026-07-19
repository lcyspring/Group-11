# PODMAN-H5-BUG-001：Mall H5 编译读取 Host node_modules

- 发现日期：2026-07-17
- 分支：`develop`
- 状态：Closed

## 现象

HBuilderX 编译器已经在 Ubuntu 26.04 容器中运行，但项目目录整体挂载后仍会读取 Host 的
`MallFrontend/node_modules`。其他成员必须先在宿主准备依赖，和 Server/Web 的容器运行时依赖模式不一致。

## 根因

旧入口只封装 HBuilderX 自带编译器，没有为 Mall 项目依赖配置 pnpm store 与 `node_modules`
named volume，也没有在编译前运行依赖容器。

## 修复

- Ubuntu 26.04 工具容器运行时执行 `pnpm install --frozen-lockfile`；
- pnpm store 与 Mall `node_modules` 分别写入专用 Podman named volume；
- HBuilderX 容器挂载同一依赖卷，遮蔽 Host 同名目录，并固定 `--network=none` 编译；
- pnpm 11 的 `core-js`、`vue-demi` 构建脚本使用显式 `allowBuilds`，不关闭供应链门禁；
- 工具链 image 不封装项目依赖，因此现有两个公开 image 无需修改。

## 回归结果

真实空 `node_modules` 卷安装 44 个包；断网 HBuilderX 编译成功，生成 167 个资源；结构、静态资源、
所有权、Git 忽略和依赖卷独立性共 10/10 项通过。
