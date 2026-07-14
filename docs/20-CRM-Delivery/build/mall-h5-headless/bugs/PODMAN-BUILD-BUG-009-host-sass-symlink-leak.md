# PODMAN-BUILD-BUG-009：宿主 Sass 绝对软链接泄漏到容器构建

- 发现日期：2026-07-14
- 级别：P1
- 状态：已关闭

## 现象

同一 uni-app CLI 在宿主构建成功，在首次精简容器中失败：

```text
[vite:css] Preprocessor dependency "sass" not found.
```

## 根因

`MallFrontend/node_modules/sass` 指向宿主绝对路径
`/opt/HBuilderX/plugins/weapp-miniprogram-ci/node_modules/sass`。首次镜像只复制
Node 和 uni-app CLI，容器内该软链接目标不存在；宿主构建成功掩盖了依赖泄漏。

## 修复

镜像从已配置的 HBuilderX 来源复制 Dart Sass 及其四个运行依赖，并放入
HBuilderX 规范的 `compile-dart-sass/node_modules` 别名路径；入口设置
`HX_APP_ROOT=/opt/HBuilderX`，由 uni-app 编译器解析自身预处理器。

## 回归

- 重建镜像后真实 H5 构建通过。
- `image.rebuild: false` 再次构建通过。
- 编译器镜像不挂载宿主 `/opt/HBuilderX`。
