# CRM 营销新增按钮国际化运行观察

日期：2026-07-17。分支：`develop`。

- Ubuntu 26.04 工具链执行 `verify:crm-marketing-i18n`：1/1；
- 页面/词典契约：4/4 页面、3/3 语言；
- Vite production build：通过；
- 使用 `runtime-local-rebuild-web.yaml` 仅替换 Web 容器；
- Web 8081：200；Server health：UP；Mall 8082：200；
- Server、MySQL、Redis、RabbitMQ、TDengine 和 Mall 未重启。
