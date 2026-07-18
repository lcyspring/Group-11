# PODMAN-RUNTIME-BUG-007：热替换 Server 跳过兼容迁移

- 发现/关闭日期：2026-07-14
- 级别：P1 / 部署一致性

## 现象与风险

`startup_mode: rebuild-server` 会封装并替换新 Server JAR，但原实现不执行 YAML 指定的
MySQL 兼容迁移清单。新增后端若同时依赖字段、菜单或权限，热替换后会形成代码与持久卷结构
不一致，典型结果是接口系统异常、权限不存在或新页面不可见。

## 修复

Server 镜像构建完成后、停止旧 Server 前，先确认 MySQL 正在运行并执行同一幂等迁移清单。
迁移失败时旧 Server 保持运行，不进入替换步骤；迁移成功后才停止并启动新 Server。

## 验证

- `bash -n podman/deploy.sh`；
- `startup_mode: rebuild-server` 真实运行通过；
- 日志确认三个清单迁移均在旧 Server 停止前成功执行；
- `crm:work-order:assign` 权限存在，Server 重启后健康状态为 `UP`；
- 工单分派、轨迹、通知和筛选真实接口均通过。
