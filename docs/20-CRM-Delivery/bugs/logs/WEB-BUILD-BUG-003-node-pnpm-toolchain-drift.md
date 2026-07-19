# WEB-BUILD-BUG-003：Web 与 Mall 工具链长期漂移

日期：2026-07-19。分支：`develop`。级别：P1/可复现构建。状态：已关闭。

## 现象与根因

Web 与 Mall 同时保留 pnpm lock/workspace、Node 测试入口和多个版本跨度较大的 Vite 插件。项目构建结果
依赖 pnpm/Node 的宿主或镜像版本，两个前端也无法共享一个明确的依赖协议。根因是工具链升级只追加
兼容入口，没有退休旧入口和锁文件。

## 修复关键

- Web 与 Mall 均改用 `deno.json`、`deno.lock` 和 `deno install --frozen`；
- 删除 pnpm lock/workspace、Node 测试 loader 以及 package script 中的 `node/pnpm/npx` 调用；
- 标准镜像固定 Ubuntu 26.04、OpenJDK 17.0.19、Maven 3.9.12、Deno 2.9.3，不包含 Node/npm/pnpm；
- 依赖只在容器运行时下载到 Podman 命名卷，Host 不安装依赖，也不把依赖烘焙进工具链镜像。

## 回归

Web Vite 8 production build 成功，Mall H5 断网编译成功；结构门禁要求两个前端的 Deno manifest/lock
存在、旧 pnpm 文件不存在、package script 不再调用退休命令。
