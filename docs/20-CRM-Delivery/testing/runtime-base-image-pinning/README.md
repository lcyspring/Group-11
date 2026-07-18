# 运行基础镜像固定测试记录

日期：2026-07-18。

| 镜像 | 当前版本 | digest 匹配 |
|---|---|---|
| Eclipse Temurin | 17.0.19+10 | 通过 |
| Nginx | 1.30.0-alpine | 通过 |
| MySQL | 8.0.46 | 通过 |
| Redis | 6.2.22-alpine | 通过 |
| RabbitMQ | 3.13.7-management-alpine | 通过 |
| TDengine | 3.3.6.0 | 通过 |

运行配置静态门禁要求六类字段和 Containerfile 默认参数均以 64 位 sha256 结尾。当前 Server 健康为
`UP`，Web/Mall 为 HTTP 200；本项未替换运行容器或升级数据格式。
