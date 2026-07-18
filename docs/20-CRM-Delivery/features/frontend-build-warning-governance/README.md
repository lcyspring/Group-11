# CRM-FEATURE-069：前端构建告警治理第一阶段

状态：第一阶段已实现。分支：`develop`。日期：2026-07-18。

Web 构建配置由 `vite.config.ts` 改为原生 ESM 的 `vite.config.mts`，插件路径统一通过项目根目录解析，
不再触发 Vite CJS Node API 弃用告警。

Mall H5 自有 `sheep/scss` 已把全局 `map-*`、`type-of`、`nth`、`append`、`zip`、`mix`、
`desaturate`、`darken` 迁移到 `sass:map/list/meta/color`。这部分不修改 HBuilderX 镜像内部依赖，
也不直接热改 `uni_modules` 第三方源码。

两端仍通过 `compile.sh` 的 Web/Mall YAML 在 Ubuntu 26.04 容器中构建。静态门禁禁止 Web 回退到
CJS 配置，也禁止 Mall 自有 Sass 重新引入已治理的全局旧函数。

剩余的 Sass `@import`、HBuilderX legacy JS API 和内置 Browserslist 数据已登记在技术栈 Plan，不能
通过屏蔽全部告警假装完成。
