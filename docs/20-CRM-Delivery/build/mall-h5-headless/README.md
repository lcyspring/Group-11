# Mall H5：Ubuntu 26.04 无图形容器编译

更新日期：2026-07-18。状态：现行。

Mall H5 直接调用 HBuilderX 内置 uni-app Vue 3/Vite CLI，不启动 IDE、Qt、X11 或 Xvfb。普通成员
统一使用公开镜像 `ghcr.io/elel-code/group-11-hbuilderx-ubuntu:26.04-5.05`，不读取或挂载 Host
HBuilderX。

## 入口

```bash
bash podman/compile.sh podman/config/build-mall-h5-ubuntu-26.04.kdl
```

命令行只接受一个 KDL 路径。Mall 项目依赖由 Ubuntu 26.04 依赖容器在运行时联网下载到独立 Podman
命名卷；随后 HBuilderX 编译容器以断网模式复用该卷。依赖不在 Host 下载，也不在工具链镜像构建时
下载。

输出为被 Git 忽略的 `MallFrontend/unpackage/dist/build/web/`。阶段二再由 `build-images.sh` 把
该产物封装为独立 Nginx 运行镜像；H5 不进入 Server JAR/WAR。

## 维护者边界

只有发布新版工具链镜像时，维护者才在专用 KDL 中设置 `image.rebuild: true` 并提供 HBuilderX
来源目录。日常共享 KDL 必须保持 `false`。本地 H5 编译不需要 DCloud 账号；云打包、uniCloud 和
加密插件不属于当前入口。

内部入口为 `podman/internal/hbuilderx-build-entrypoint.sh` 与
`podman/internal/mall-dependencies-entrypoint.sh`。历史无图形编译缺陷保留在本目录 `bugs/`，
回归结果见 `TEST-REPORT.md`。
