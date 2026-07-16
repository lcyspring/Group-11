# CRM 运行安全基线测试结果

日期：2026-07-17。分支：`develop`。

| 验收项 | 结果 |
|---|---|
| Ubuntu 26.04 Server 全 reactor 构建 | 通过，37 个 reactor 模块全部成功 |
| `SecurityPropertiesTest` | 3/3，通过 |
| `MitedtsmWebAutoConfigurationTest` | 2/2，通过 |
| rootless Podman health | `UP` |
| Actuator env、OpenAPI、Swagger UI、Druid | 4/4 未暴露；兼容统一 `code=404` 响应 |
| 历史 `Bearer test1` Mock Token | 拒绝，统一响应 `code=401` |
| CORS | 配置来源放行，非可信来源无 allow-origin |
| 显式安全与数据源容器环境 | 8/8 |
| 公共/local profile 内联凭据门禁 | 通过 |

运行过程中同时验证：未配置 AI Provider 不会触发默认模型 Bean；TDengine 账号密码由运行 YAML
注入，Server、Web、Mall 分别在 8080、8081、8082 保持可用。
