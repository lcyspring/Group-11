# CRM 运行安全基线测试计划

1. 验证 BCrypt 默认值、弱强度拒绝和 Mock 密钥条件校验；
2. 验证 CORS 精确来源映射及默认无来源；
3. 在 Ubuntu 26.04 容器执行框架专项、JaCoCo 和 Server build；
4. 校验唯一运行 KDL 的全部安全与集成字段；
5. 真实服务验证 health 可用，env、Swagger、Druid 不可匿名访问；
6. 使用历史 Mock Token 验证无法登录；
7. 预检可信/非可信 Origin 的 CORS 正反向响应；
8. 静态扫描公共/local profile，拒绝内联集成密钥。
