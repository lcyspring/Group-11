# Podman 三阶段交付配置覆盖

本专项是 Shell/KDL 流程覆盖，不使用 JavaScript 或 JaCoCo 行覆盖率冒充脚本覆盖率。

| 协议 | 覆盖 |
|---|---|
| 编译阶段 | Server/Init/Web、Mall H5 两类容器入口 |
| 镜像封装目标 | InitService、Server、Web、Mall 4/4；MySQL 使用官方镜像 |
| 数据库 provision | SQL root、bootstrap、compatibility、dataset 4/4 |
| 启动模式 | check、replace、fast、frontends-only、replace-server、replace-web、replace-mall 7/7 |
| 镜像来源 | auto、archive、pull 3/3 配置分支 |
| 命令行契约 | 三阶段入口均为单 KDL 路径 |
