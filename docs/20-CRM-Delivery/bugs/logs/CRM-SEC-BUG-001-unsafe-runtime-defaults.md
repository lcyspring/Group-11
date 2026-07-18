# CRM-SEC-BUG-001：Podman CRM 运行环境沿用不安全开发默认值

## 现象

CRM 观察环境使用 `local` profile，Mock Token 登录开启、BCrypt 强度为 4、CORS 允许任意来源携带
凭证，Actuator 暴露全部端点，Swagger/Knife4j 与无认证 Druid 控制台也默认开启。

## 根因

部署 YAML 只声明容器、端口和基础设施，没有把应用安全策略作为运行契约；框架把开发便利值写成
代码默认值，Podman 启动又只传 Spring profile，导致环境无法独立收紧策略。

## 修复

- Podman YAML 显式声明 Mock、BCrypt、XSS、CORS、API 文档、Druid、Actuator、API 加密和验证码；
- `deploy.sh` 校验 BCrypt 10～16、Actuator 仅 health/info、凭证 CORS 禁止全通配；
- Mock 默认关闭，启用时必须提供非空显式密钥；BCrypt 代码默认提高到 10；
- CORS 从硬编码 `*` 改为配置绑定，默认无可信来源；
- 运行容器只暴露 health/info，并关闭文档与 Druid 控制台。

## 验证

Ubuntu 26.04 Server 全 reactor 构建通过；Security 3/3、CORS 2/2。rootless Podman 真实验收
确认 health `UP`，env/OpenAPI/Swagger/Druid 4/4 未暴露，历史 Mock Token 被拒绝，可信与非可信
Origin 正反向结果正确，显式安全与数据源环境 8/8。

## 分支

`develop`
