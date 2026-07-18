# Podman 三阶段交付测试结果

日期：2026-07-18。当前状态：通过。

| 检查 | 结果 |
|---|---|
| `compile.sh` 白名单/黑名单目标选择 | 通过；59 份编译 KDL 完成协议迁移，非法、重复和空工作选择均被拒绝 |
| 单 KDL 四目标完整编译 | 通过；Server、InitService、Web、Mall H5 在同一次命令中依次成功 |
| CRM 测试与 JaCoCo | 532/532；行 48.71%、分支 42.97%、方法 37.97%，报告已生成 |
| Web 专项测试覆盖率 | 3/3；行 96.45%、分支 90.63%、函数 100% |
| HBuilderX 引擎真实分派 | 10/10；容器运行时依赖、断网编译、167 个资源、Host 所有权和 Git 忽略均通过 |
| `build-images.sh` Bash 语法 | 通过 |
| `deploy.sh` Bash 语法 | 通过 |
| 镜像封装 KDL-only 预检 | 通过 |
| 容器启动/替换 KDL-only 预检 | 通过 |
| `deploy.sh` 无 `podman build` 和 Host 产物读取静态门禁 | 通过 |
| 四目标运行镜像封装 | 通过；InitService、Server、Web、Mall 4/4；数据库不封装镜像 |
| 数据库部署边界 | 通过；显式 SQL root、bootstrap/compatibility/dataset manifest、stdin provision |
| 完整三阶段真实替换 | 通过；持久卷保留，BPM 模型恢复成功，8080/8081/8082 健康 |
| 本轮单组件镜像封装 | 通过；Server、Web 分别封装成功 |
| 本轮完整替换 | 通过；兼容迁移及 9 个 BPM 模型幂等恢复，Server `UP`、Web 200、Mall 200 |
| 旧入口及 Host 编译依赖脚本 | 已删除；无失效引用，测试/验证资产保留在结构化目录 |
| 脚本目录与 `--replace` | 通过；根目录仅四个日常入口，完整 Pod 使用 `pod create --replace`，容器使用 `run --replace` |
| 移动后容器入口权限 | 通过；三个 `internal/*entrypoint.sh` 的语法和可执行位均纳入门禁 |
| 移动后真实 CRM 验收 | 通过；营销链接点击白名单、302、并发安全计数、汇总和清理 7 项均通过 |

构建仍报告上游 Vite CJS API、Browserslist 数据和 Sass legacy API/`@import` 弃用警告；本轮不影响
产物与运行验收，但应在前端依赖升级中逐步迁移，不能误记为零告警。
