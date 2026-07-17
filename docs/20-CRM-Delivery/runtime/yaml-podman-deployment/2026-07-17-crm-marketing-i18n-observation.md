# CRM 营销新增按钮国际化运行观察

日期：2026-07-17。分支：`develop`。

- Ubuntu 26.04 工具链执行 `verify:crm-marketing-i18n`：1/1；
- 页面/词典契约：4/4 页面、3/3 语言；
- Vite production build：通过；
- 使用 `runtime-local-rebuild-web.yaml` 仅替换 Web 容器；
- Web 8081：200；Server health：UP；Mall 8082：200；
- Server、MySQL、Redis、RabbitMQ、TDengine 和 Mall 未重启。

## 二次运行复验

针对页面仍显示 `common.create` 的反馈，再次完成相同 Ubuntu 26.04 构建和 Web 单服务替换。
运行容器与工作区入口文件 SHA-256 均为
`bfd5279d84f94ec1e2b800a2995e9232637fc51c9f0efb759bf78a6bc6e08ad6`；两个目标页面的生产块
均使用 `action.create`，精确 `common.create` 残留为 0。旧标签页需强制刷新以释放此前加载的动态块。
