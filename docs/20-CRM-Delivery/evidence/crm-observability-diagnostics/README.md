# CRM 可观测运行证据

日期：2026-07-17。分支：`develop`。

执行入口：

```bash
bash ./podman/operations/diagnostics/collect-crm-diagnostics.sh ./podman/config/crm-diagnostics-local.yaml
```

正常运行结论为 PASS：Server `UP`、Web/Mall `200/200`、7/7 容器运行、重启 0、近 30 分钟
ERROR/FATAL 0 行、磁盘可用 72%、内存使用 27.99%、MySQL 连接使用 5.96%。

阈值演练把磁盘最低可用空间临时设为 100%，正确返回 FAIL 和 `disk_free=72%`，未停止或重启服务。
原始日志、容器指标、MySQL 指标与 tar.gz 位于 `podman/diagnostics/`，可能包含运行上下文，已 Git ignore。
