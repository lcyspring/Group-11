# Web 语言分包完整性测试计划

- 验证 Vue i18n 插件不处理 TypeScript 语言聚合模块；
- 验证中文、英文、阿拉伯文生产入口映射到独立且非空的分包；
- 将产物检查接入 Ubuntu 26.04 编译阶段，分包异常时禁止封装镜像；
- 执行专项测试、ESLint 和 Web production build。
