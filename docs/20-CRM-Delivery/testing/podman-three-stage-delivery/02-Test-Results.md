# Podman 三阶段交付测试结果

日期：2026-07-18。当前状态：通过。

| 检查 | 结果 |
|---|---|
| `compile.sh` 统一入口及 `build.engine` 分派 | 通过；59 份编译 YAML 均显式声明引擎 |
| HBuilderX 引擎真实分派 | 10/10；容器运行时依赖、断网编译、167 个资源、Host 所有权和 Git 忽略均通过 |
| `build-images.sh` Bash 语法 | 通过 |
| `deploy.sh` Bash 语法 | 通过 |
| 镜像封装 YAML-only 预检 | 通过 |
| 容器启动/替换 YAML-only 预检 | 通过 |
| `deploy.sh` 无 `podman build` 和 Host 产物读取静态门禁 | 通过 |
| 五目标运行镜像封装 | 通过；MySQL、InitService、Server、Web、Mall 5/5 |
| 完整三阶段真实替换 | 通过；持久卷保留，BPM 模型恢复成功，8080/8081/8082 健康 |
| 本轮单组件镜像封装 | 通过；Server、Web 分别封装成功 |
| 本轮完整替换 | 通过；兼容迁移及 9 个 BPM 模型幂等恢复，Server `UP`、Web 200、Mall 200 |
| 旧入口及 Host 编译依赖脚本 | 已删除；无失效引用，测试/验证资产保留在结构化目录 |
