# Mall H5：Ubuntu 26.04 无图形容器构建

更新日期：2026-07-14

## 结论

Mall H5 已改为直接调用 HBuilderX 内置的 uni-app Vue3/Vite CLI，不启动
HBuilderX IDE、Qt、X11 或 Xvfb。宿主机日常只需要 rootless Podman；已有的
自包含编译镜像可在不读取、不挂载 `/opt/HBuilderX` 的情况下重复构建。

## 使用方式

```bash
cd podman
bash ./build-mall-h5-in-ubuntu.sh ./config/build-mall-h5-ubuntu-26.04.yaml
```

命令行只接受一个 YAML 路径。YAML 显式配置基础镜像、编译镜像、是否重建、
HBuilderX 组件来源、平台、清理策略、离线网络和资源上限。

首次构建或 `image.rebuild: true` 时，宿主需要存在配置的 HBuilderX 来源目录；
镜像仅复制其 Node、`uniapp-cli-vite` 和 Dart Sass 依赖。`image.rebuild: false`
且镜像已存在时，不访问该来源目录。

## 账号与网络

本地 H5/web 编译不需要 DCloud/HBuilderX 账号，也不读取登录状态。构建网络
固定为 `network.mode: none`。云打包、uniCloud 发布和付费/加密插件不属于
本入口范围；未来启用时，YAML 只能引用已忽略的凭据文件，不能提交明文密码。

## 产物策略

- 输出：`MallFrontend/unpackage/dist/build/web/`
- 产物由 `unpackage/` 规则统一忽略。
- 历史跟踪的 386 个生成文件已从 Git 索引移除，本地文件保留。
- 部署镜像打包前必须先运行本构建入口。

## 相关实现

- `podman/build-mall-h5-in-ubuntu.sh`
- `podman/hbuilderx-build-entrypoint.sh`
- `podman/Containerfile.hbuilderx-ubuntu`
- `podman/hbuilderx.containerignore`
- `podman/config/build-mall-h5-ubuntu-26.04.yaml`
- `podman/tests/mall-h5-build/run.sh`

Bug 记录位于本目录的 `bugs/`，测试证据见 `TEST-REPORT.md`。
