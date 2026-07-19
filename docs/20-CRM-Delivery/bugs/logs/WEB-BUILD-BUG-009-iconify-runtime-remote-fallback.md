# WEB-BUILD-BUG-009：图标运行时回退到远程 Iconify API

日期：2026-07-19。分支：`develop`。级别：P1/离线可用性与隐私。状态：已关闭。

## 现象与根因

删除旧 Purge Icons 插件后，组件改用 `@iconify/iconify` 的 `renderSVG`。该包不自带图标数据；找不到
`ep:*` 等图标时会创建 `data-icon` 节点并访问 Iconify、SimpleSVG、UniSVG API。生产构建能成功，
但离线环境图标空白，并产生未配置的浏览器外联。初版 `offline` export 仍把 API 地址和冗余网络代码
打入产物，也不能作为完全隔离证据。

## 修复关键

- 生成并提交 40 个集合、2051 个实际/可选择图标的离线快照；生成阶段才允许经显式 Host 代理访问官方 API；
- 每次 Web build 断网校验源码、数据库菜单和 IconSelect 候选均能在快照解析；
- 删除 `@iconify/iconify` 浏览器运行时，改用 `@iconify/utils` 从本地 JSON 生成 SVG；
- 修复首次挂载时 watch 未触发的问题；缺图只标记本地 `data-missing-icon`，不发网络请求；
- 修正上游已不存在的 `ep:question` 为 `ep:question-filled`。

## 回归

离线快照校验通过，Web 图标专项合计 5/5；生产产物扫描不到三个 Iconify API 域名或
`@iconify/iconify`，Vite 8 production build 成功且 warning 为 0。
