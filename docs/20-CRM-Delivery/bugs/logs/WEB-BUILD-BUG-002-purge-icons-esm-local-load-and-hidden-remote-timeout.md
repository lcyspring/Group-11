# WEB-BUILD-BUG-002：图标插件本地加载失败并隐式远程超时

- 日期：2026-07-18
- 分支：`develop`
- 状态：已修复

## 问题

Ubuntu 26.04 容器执行 Web 生产构建时，Vite 在 `purge-icons` 阶段长时间无输出，最终出现
`ETIMEDOUT`；强制使用本地图标集合后又出现动态 `require` 不受支持。

## 根因

`@purge-icons/core@0.10.0` 的 ESM 产物无法执行本地图标 JSON 的动态 `require`。默认 `auto`
模式捕获该错误后会隐式访问 GitHub 原始文件地址；项目涉及多个图标集合，并发远程请求容易因网络波动超时。

## 修复

- 使用包已公开的 CommonJS 导出加载插件，使本地图标 JSON 的动态 `require` 正常工作；
- `iconSource` 显式设为 `local`，图标数据来自锁定的 `@iconify/json` 依赖；
- 增加构建配置测试，禁止恢复无参数 `PurgeIcons()` 或隐式远程回退。

## 验证

Ubuntu 26.04 容器专项 4/4 通过；图标配置测试覆盖率为 100%。完整 Web 生产构建与三语言包完整性校验通过。

