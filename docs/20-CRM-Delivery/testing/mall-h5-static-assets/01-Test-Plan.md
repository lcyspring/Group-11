# 商城 H5 静态资源测试计划

1. 扫描商城快捷菜单中的 `/static/` 图标字面路径；
2. 每个路径必须同时存在于源码 `MallFrontend/static` 和 H5 最终产物；
3. 使用 Ubuntu 26.04 HBuilderX 无 GUI 构建路径生成 H5；
4. 部署后对全部快捷入口图标执行 HTTP 200 与内容类型检查。
