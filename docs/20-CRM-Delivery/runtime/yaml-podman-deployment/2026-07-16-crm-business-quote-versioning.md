# 2026-07-16 商机报价版本运行验收

- 分支：`develop`
- 构建：Ubuntu 26.04 容器
- 运行：rootless Podman
- 配置：显式 YAML，命令行只传配置路径

`runtime-local-rebuild-server.yaml` 应用完整兼容迁移并重启 Server，8080 健康检查通过，8081、
8082 保持运行。报价迁移把合同金额列从两位小数扩至 `decimal(24,6)`；完整迁移再次执行未产生
重复表、列、索引或数据。

最终完全 YAML 驱动的真实 API 验收创建商机 20、报价 V1=21、V2=22、合同 27。V1 锁定、V2 重开、旧版本不可变、
赢单门槛、合同来源快照、客户端伪造拒绝和重复转换幂等全部通过。脚本随后删除商机、两版报价、
明细、动作、合同、权限和轨迹，数据库残留合计为 0。

管理端 production bundle 已包含 YAML 注入的退休媒体源；8081 favicon 返回 200。Server、Web、
Mall、MySQL、Redis、RabbitMQ 和 TDengine 均保持运行，供人工观察。
