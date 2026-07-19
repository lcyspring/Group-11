# WEB-BUILD-BUG-001：Vite 配置触发 CJS Node API 弃用告警

日期：2026-07-18。分支：`develop`。优先级：P2/维护。状态：已关闭。

## 现象与根因

Web 容器构建成功，但专项和生产构建各输出一次 `The CJS build of Vite's Node API is deprecated`。
项目没有声明 ESM 包类型，`vite.config.ts` 被 Vite 5.1 按 CJS 配置装载；插件配置还依赖 CJS 的
`__dirname`。

## 修复关键

- 配置入口改为 Vite 原生识别的 `vite.config.mts`；
- i18n 资源目录复用基于 `process.cwd()` 的根路径解析，不依赖 `__dirname`；
- 不把整个 Web 包强制改成 `type=module`，避免无关 Node 脚本一次性失效。

## 验证

相同 Ubuntu 26.04 YAML 完整重跑：CJS 告警 2 → 0，媒体专项 3/3，生产构建成功。
