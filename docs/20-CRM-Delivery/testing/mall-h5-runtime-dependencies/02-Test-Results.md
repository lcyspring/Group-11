# Mall H5 容器运行时依赖测试结果

日期：2026-07-17。分支：`develop`。

- 空依赖卷安装：44 个包，成功；
- pnpm 11 供应链检查：通过，允许构建包显式限定为 `core-js`、`vue-demi`；
- HBuilderX 编译网络：`none`；
- 构建结果：成功，167 个资源；
- 自动验收：10/10；
- Host `MallFrontend/node_modules` 被 named volume 遮蔽，不参与依赖安装或模块解析。
