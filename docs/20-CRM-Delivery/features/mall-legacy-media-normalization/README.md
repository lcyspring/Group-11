# Mall 遗留媒体规范化

该扩展让离线/本地 Mall 部署不再依赖已退役的演示文件域名。

- 输入由 `media.legacy_origins` 和 `media.legacy_fallback` 显式配置；
- 内置静态资源映射到同源 `/static/**`；
- 无法恢复的历史上传媒体显示明确占位图；
- 活跃的其他远程媒体 URL 保持不变；
- 相似但非同一来源的恶意/错误域名不会被误匹配。
- API JSON 在响应入口递归规范化，组件级 `cdn()` 作为第二道防线；
- H5 使用打包后的本地 favicon，不再发起默认 `/favicon.ico` 请求。
