# CRM-FEATURE-070：运行基础镜像精确固定

状态：已实现。分支：`develop`。日期：2026-07-18。

Temurin、Nginx、MySQL、Redis、RabbitMQ 和 TDengine 的 tracked KDL/Containerfile 均采用“精确版本标签
+ sha256 digest”，避免 `stable`、大版本标签在不同时间解析为不同镜像。归档文件名仍保持人类可读，
加载后按 digest 校验同一内容。

这只是固定当前基线，不隐式升级 Redis/JDK/MySQL 大版本；任何升级仍需独立兼容性、数据恢复和业务回归。
