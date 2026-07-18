# 前端构建告警治理测试结果

执行日期：2026-07-18。环境：Ubuntu 26.04 公共工具链与 HBuilderX 5.05 公共镜像。

| 项目 | 迁移前 | 迁移后 | 结果 |
|---|---:|---:|---|
| Web Vite CJS Node API | 2 | 0 | 已关闭 |
| Mall 日志行数 | 28732 | 4624 | 降低 83.9% |
| Mall 全局旧函数显式告警 | 895 | 0 | 已关闭 |
| Mall Sass `@import` | 366 | 366 | 待模块化迁移 |
| HBuilderX legacy JS API | 178 | 178 | 待工具链升级 |
| HBuilderX Browserslist 数据 | 1 | 1 | 待工具链升级 |

- Web 媒体专项 3/3，生产构建成功；
- Mall 媒体专项 7/7，行/分支/函数覆盖率 100%，H5 产物生成成功；
- 依赖均由容器运行时解析，Host 未安装或调用 Node/pnpm/HBuilderX 构建链。
