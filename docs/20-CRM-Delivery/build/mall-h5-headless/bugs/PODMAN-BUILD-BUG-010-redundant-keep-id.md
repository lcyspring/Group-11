# PODMAN-BUILD-BUG-010：H5 构建重复指定 rootless 用户映射

- 发现日期：2026-07-14
- 级别：P3
- 状态：已关闭

## 现象

构建命令同时使用 `--userns=keep-id` 和 `--user "$(id -u):$(id -g)"`，试图
保证挂载目录产物归当前宿主用户。

## 根因

rootless Podman 的默认 user namespace 已将容器 UID 0 映射为启动 Podman 的
宿主 UID。两项参数重复了默认所有权语义，并让进程以镜像中未声明的数字 UID
运行，增加无收益的兼容复杂度。

## 修复

删除 `--userns` 和 `--user`，使用 rootless Podman 默认映射。YAML 不配置 Linux
账号；这不是可变业务参数。

## 回归

结构化集成测试在真实构建后比较 `index.html` 的宿主 UID 与 `id -u`，必须一致。
