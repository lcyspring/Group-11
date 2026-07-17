# Mall H5 容器运行时依赖测试计划

1. 从空 named volume 运行 pnpm 冻结锁文件安装；
2. 验证依赖容器为 Ubuntu 26.04，Host 不执行 pnpm；
3. 验证 pnpm store 与 `node_modules` 使用独立 Podman 卷；
4. 验证 HBuilderX 编译挂载同一依赖卷并使用 `network=none`；
5. 验证入口、静态资源、产物所有权和 Git 忽略规则。
