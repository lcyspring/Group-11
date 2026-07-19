# 前端构建告警覆盖记录

| 告警族 | 来源归属 | 本阶段结论 |
|---|---|---|
| Vite CJS Node API | Web 项目配置 | 已迁移，0 条 |
| Sass 全局 map/list/meta/color 函数 | Mall `sheep/scss` | 已迁移，0 条 |
| Sass `@import` | Mall 项目与 `uni_modules` | 已记录，下一阶段迁移项目源码 |
| Sass legacy JS API | HBuilderX 5.05 内置 uni-app CLI | 当前镜像不可原地修改，待新工具链验证 |
| Browserslist 数据过期 | HBuilderX 5.05 内置依赖 | 当前镜像不可原地修改，待新工具链验证 |

功能回归覆盖：Web 专项 3/3；Mall 专项 7/7 且行、分支、函数覆盖率均为 100%。构建日志计数不等同
业务代码覆盖率，因此未把告警减少量写成测试覆盖率。
