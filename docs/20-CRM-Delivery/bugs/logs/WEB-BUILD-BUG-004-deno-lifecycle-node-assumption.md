# WEB-BUILD-BUG-004：依赖 lifecycle 脚本隐式假定系统 Node

日期：2026-07-19。分支：`develop`。级别：P2/供应链。状态：已关闭。

## 现象与根因

Deno 安装 npm 依赖时发现部分第三方包声明 build/postinstall 脚本，这些脚本直接假定 `node` 命令存在。
若用 Node shim 或在标准镜像中补装 Node，会重新引入双运行时和未审计的安装期代码执行。

## 修复关键

- 不增加 Node shim，也不为 lifecycle 兼容向标准镜像安装 Node/npm；
- Deno 默认忽略第三方安装脚本，依赖解析仍由冻结锁文件约束；
- 删除依赖安装阶段不再需要的旧 Vite 插件，图标改用提交的离线快照与纯 SVG utils；
- 以 Web 生产构建、Mall H5 实际编译和 Vue 3 运行内容验证代替盲目批准 lifecycle 脚本。

## 回归与边界

Web 6807 模块完成生产构建，Mall 167 个资产完成断网编译。依赖升级若确实需要原生安装脚本，必须先
单独审计并形成显式允许清单，不能恢复全局 Node 兼容层。
