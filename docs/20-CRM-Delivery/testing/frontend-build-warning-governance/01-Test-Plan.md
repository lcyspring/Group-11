# 前端构建告警治理测试计划

1. 用 `build-web-ubuntu-26.04.yaml` 完整执行 Web 专项、ESLint 和生产构建；
2. 对比迁移前后 Vite CJS、Sass、Browserslist 告警类型和次数；
3. 用 `build-mall-h5-ubuntu-26.04.yaml` 在运行时安装依赖并执行 HBuilderX H5 构建；
4. Mall 媒体 URL 专项必须 7/7，通过率与覆盖率均保持 100%；
5. 扫描 Mall 自有 Sass，不允许重新出现已迁移的全局旧函数；
6. 区分项目源码、`uni_modules` 和 HBuilderX 内置工具链，剩余项必须显式登记。
