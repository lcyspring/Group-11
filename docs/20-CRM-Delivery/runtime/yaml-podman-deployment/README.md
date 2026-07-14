# YAML 驱动的 Podman 运行配置

## 背景

旧 `podman/up.sh` 和 `down.sh` 同时使用命令行选项、环境变量默认值及脚本正文
常量控制部署。相同命令可能因宿主环境不同产生不同结果，也无法从单一文件审计
端口、镜像、凭据和数据删除意图。

## 本次实现

- 新增 `podman/config/runtime-local-check.yaml`，完整声明运行期行为；
- 新增不使用 `eval` 的两层 YAML 标量读取库；
- `up.sh`、`down.sh`、`image-archives.sh` 均只接受一个 YAML 路径；
- 启动/停止模式、删除卷意图均迁移到 YAML；
- Pod、容器、卷、镜像、归档、端口、服务凭据、Spring profile、代理和健康
  检查参数均显式化；
- 相对归档路径按配置文件目录解析；
- 新增安全的启动与停止 `check` 模式，不改变 Pod/卷；
- 本机状态配置 `podman/config/runtime-local.yaml` 加入 `.gitignore`，避免提交真实
  凭据；
- README 与中文部署指南删除旧 CLI/环境变量示例。

## 配置约束

解析器只支持顶层键和一层子映射，不支持列表、锚点或多行标量。缺失必填值、
重复键、非法缩进层级、非严格 `true/false`、非法端口和非法操作模式均失败。
配置文本不作为 Shell 代码执行。

## 模式

- 启动：`check`、`full`、`fast`、`no-build`、`frontends-only`、`rebuild-server`、`rebuild-web`、`rebuild-mall`；
- 停止：`check`、`stop`；
- 镜像归档：`check`、`save`、`pull-save`；
- 删除卷：停止为 `stop` 且 `remove_volumes_on_down: true` 时才执行。

## 数据库字符集边界

MySQL 表字符集和客户端连接字符集必须同时正确。首次初始化固定使用 utf8mb4，运行期兼容迁移、
schema 探针和文件存储配置更新使用 YAML 的 `mysql.character_set`，防止中文 SQL 被按 latin1
双重编码。存量可见数据由 manifest 中的幂等修复迁移按稳定键纠正。

`check` 模式不改变 Pod、卷和数据；`full` 及热替换模式会按各自定义构建或替换容器。删除卷仍只在
停止为 `stop` 且 `remove_volumes_on_down: true` 时发生。
