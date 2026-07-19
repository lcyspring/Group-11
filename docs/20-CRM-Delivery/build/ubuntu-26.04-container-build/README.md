# Ubuntu 26.04 容器编译基线

更新日期：2026-07-19。状态：现行。

Server、InitService、Web、自动化测试与覆盖率统一在公开工具链镜像
`ghcr.io/elel-code/group-11-build-ubuntu:26.04-deno-2.9.3` 中执行。普通成员不重建工具链镜像，Host 只需
Git 与 rootless Podman。

## 入口

```bash
bash podman/compile.sh podman/config/build-ubuntu-26.04.kdl
bash podman/compile.sh podman/config/test-crm-ubuntu-26.04.kdl
bash podman/compile.sh podman/config/build-web-ubuntu-26.04.kdl
```

命令行只接受一个 KDL 路径。编译目标、清理、测试、覆盖率、缓存、代理和资源限制均来自 KDL。
项目 Maven/Deno 依赖由容器运行时下载到 Podman 命名卷，Host 不安装项目工具链或依赖。

## 工具链

- Ubuntu 26.04；
- Java 17、Maven 3.9.12；
- Deno 2.9.3；标准镜像不包含 Node、npm、pnpm；
- Web 使用 Vite 8.1.5，测试使用 Deno Test/Coverage，并按 KDL 输出 LCOV；
- UTF-8 locale；
- 固定公开工具链镜像。

容器内执行入口为 `podman/internal/ubuntu-build-entrypoint.sh`；它属于内部实现，成员不应直接调用。
完整字段和流程见 `podman/OPERATIONS_ZH.md` 与 `podman/config/KDL_FIELDS_ZH.md`。

## 已关闭的兼容问题

历史上已处理 Host loopback 代理、非 UTF-8 locale、旧 pnpm 版本偏差、clean 删除覆盖率、无 TTY
`node_modules` 和旧 store 挂载位置问题。正式记录位于
`docs/20-CRM-Delivery/bugs/logs/PODMAN-BUILD-BUG-001-*` 至 `PODMAN-BUILD-BUG-006-*`。

该基线不恢复 Host 编译、Docker/Compose、旧 YAML 或不支持软链接目录的暂存复制逻辑。
