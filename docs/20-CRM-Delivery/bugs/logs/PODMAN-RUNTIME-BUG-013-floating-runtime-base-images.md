# PODMAN-RUNTIME-BUG-013：运行基础镜像使用浮动标签

日期：2026-07-18。分支：`develop`。优先级：P1/可复现。状态：已关闭。

## 现象与根因

Redis `6-alpine`、RabbitMQ `3-management-alpine`、MySQL `8.0`、Nginx `stable-alpine` 和 Temurin
`17-jdk` 会随上游移动；相同 YAML 在不同日期可能拉到不同二进制，版本记录和回滚无法精确对应。

## 修复关键与验证

- 六类镜像全部使用精确版本标签加仓库 sha256 digest；
- Containerfile 默认值、运行检查配置、运行镜像示例和中文来源清单同步；
- 静态门禁拒绝 tracked 配置重新出现无 digest 的基础镜像；
- 本机六个 digest 均与当前镜像匹配，Server `UP`，Web/Mall HTTP 200。
