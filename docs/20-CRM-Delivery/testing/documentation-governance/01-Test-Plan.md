# 文档治理测试计划

1. 校验根 README、当前交付目录和 Podman 中文文档的相对 Markdown 链接；
2. 拒绝本机绝对路径链接；
3. 排除历史 Bug 日志和 daily 报告后，拒绝旧 Podman YAML、旧启动模式、旧测试树和已删除文档目录引用；
4. 校验入口只接受一个 KDL 路径，并先通过 dasel 解析该配置；
5. 测试只读，不修改 Pod、容器、卷、数据库或构建产物。
6. 确认未使用的 Docker Compose 文件和 Spring Docker profile 不再返回代码树。
