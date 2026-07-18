# CRM Podman 运行安全基线

CRM 运行服务的安全策略现由调用 `deploy.sh` 时唯一指定的 KDL 文件控制：

- Mock 登录默认关闭，启用必须提供显式密钥；
- BCrypt 强度限定 10～16；
- XSS 过滤、可信 CORS 来源、请求头、方法和缓存时间显式配置；
- API 文档、Druid 控制台、Actuator 暴露范围显式配置；
- 公众号、小程序、社交登录、地图及支付回调从同一 KDL 注入；
- MySQL、RabbitMQ 与 TDengine 账号复用 KDL 基础设施事实源，不再依赖源码口令；
- 公共配置中的可选 AI/快递 Provider 默认关闭并只接受外部凭据。

本地观察配置允许关闭验证码以支持自动验收，但该选择同样显式写入 KDL；它不恢复 Mock Token，
也不放宽管理端点和跨域策略。
