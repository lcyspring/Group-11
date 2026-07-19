# WEB-BUILD-BUG-005：Vite 8 暴露旧构建契约和源码缺陷

日期：2026-07-19。分支：`develop`。级别：P1/前端构建。状态：已关闭。

## 现象与根因

Vite 升级后，旧 PostCSS 模块格式、数组 alias、对象式 `manualChunks`、plain CSS 的 `//` 注释、IE
`*zoom` hack 和一处非 UTF-8 排行组件注释阻断或污染生产构建。它们过去被旧工具链宽松处理，未形成
标准门禁。

## 修复关键

- PostCSS 显式使用 CommonJS 配置文件，Vite alias 改为标准对象；
- `manualChunks` 改为按模块 ID 返回 chunk 名的函数；
- 删除无效 IE hack，修正非法 CSS 注释和非 UTF-8 文本；
- 将 Vue、Vite、UnoCSS、i18n 与 BPMN 相关插件升级为相容版本，删除四个退休 Vite 插件。

## 回归

Vite 8.1.5 转换 6807 个模块并完成 production build；中文、英文、阿拉伯文语言分包均通过完整性检查。
