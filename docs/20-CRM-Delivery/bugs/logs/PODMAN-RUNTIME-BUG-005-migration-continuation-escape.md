# PODMAN-RUNTIME-BUG-005：迁移清单命令续行符转义错误

## 现象

完整启动在执行第一个兼容迁移时失败：

`crun: executable file '\\' not found in $PATH`

基础设施已启动，Server/Web/Mall 尚未创建。

## 根因

将单文件迁移升级为清单循环时，`podman exec` 的 Shell 续行符误写成两个反斜杠；
Bash 因此把一个字面量反斜杠作为容器内可执行命令传给 Podman。

## 修复

恢复为单反斜杠续行，并用 `bash -n`、check 模式和真实 full 启动共同验证。

## 数据安全

失败发生在幂等 SQL 执行前，MySQL、Redis、RabbitMQ、TDengine 持久卷均保留，未执行
删除卷或破坏性数据库操作。修复后直接重跑同一 YAML 即可恢复应用服务。
