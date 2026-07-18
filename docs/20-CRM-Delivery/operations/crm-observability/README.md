# CRM 可观测与故障处置手册

## 日常检查

复制共享模板为本机配置，填写真实 MySQL 只读/运维账号后执行：

```bash
cp ./podman/config/crm-diagnostics.example.yaml ./podman/config/crm-diagnostics-local.yaml
bash ./podman/operations/diagnostics/collect-crm-diagnostics.sh ./podman/config/crm-diagnostics-local.yaml
```

命令行不能传账号、阈值或输出路径；所有差异只修改 ignored YAML。正常执行不改变 Pod、容器、卷、
数据库和业务数据。

## SLI 与默认阈值

| SLI | 默认阈值 | 含义 |
|---|---:|---|
| Server health | `UP` | Spring 应用与健康组件可用 |
| Web/Mall HTTP | 200/200 | 两个用户入口可访问 |
| 服务容器 | 7/7 running | MySQL、Redis、RabbitMQ、TDengine、Server、Web、Mall 均运行 |
| 最大重启次数 | 0 | 当前容器实例没有非预期重启 |
| 最近错误级日志 | ≤ 20 行/30 分钟 | ERROR/FATAL 粗粒度异常预算；需结合上下文判断 |
| 项目磁盘可用 | ≥ 10% | 防止日志、数据库和镜像写满 |
| 主机内存使用 | ≤ 90% | 防止持续内存压力和 OOM |
| MySQL 连接使用 | ≤ 80% | `Threads_connected / max_connections` |

开发机阈值是最低运行门，不是生产 SLA。共享/生产环境应按资源、流量和告警系统重新签署 YAML。

## 处置顺序

1. 查看 `SUMMARY.md` 的失败项，不先重启服务；
2. 健康失败时检查 `server-health.json` 和 Server 最近日志；
3. 容器缺失或退出时检查 `container-state.tsv`、对应日志和 `container-stats.txt`；
4. MySQL 连接过高时检查 `mysql-metrics.tsv`，区分连接泄漏、慢请求和容量不足；
5. 磁盘不足时先定位镜像、归档、构建缓存和日志，不删除数据库卷；
6. 内存过高时核对 Server/MySQL 资源采样和进程趋势，再决定限流、扩容或受控重启；
7. 修复后使用同一 YAML 复跑，PASS 后保留两次摘要用于对比。

## 常见故障

| 失败项 | 优先核查 | 禁止的快捷处理 |
|---|---|---|
| `Server health != UP` | Server 日志、MySQL/Redis 状态、最近部署 | 直接删卷或覆盖真源库 |
| Web/Mall 非 200 | Nginx 日志、产物入口、对应运行镜像 | 在宿主临时改构建产物掩盖问题 |
| 容器非 running/重启 | 容器日志、内存、端口、依赖顺序 | 未留证据就循环重启 |
| ERROR/FATAL 超额 | 按时间关联 API、SQL、消息与审批 | 只提高阈值而不分类错误 |
| MySQL 连接超额 | 活跃连接、慢请求、连接池配置 | 强杀数据库或删除数据卷 |
| 磁盘/内存超额 | 构建缓存、ignored 归档、日志、容器资源 | 删除未知目录或业务附件 |

## 诊断包安全

`podman/diagnostics/` 中可能包含用户名、业务 ID、请求路径和异常上下文，只用于本机故障定位。发送给
其他人前必须复核并脱敏；不要把压缩包、真实 YAML 或原始日志加入 Git。提交仓库的证据只保留汇总指标。

## 不停服务阈值演练

演练只调高 YAML 阈值，不制造真实服务故障。例如把 `min_disk_free_percent` 临时设为 100，脚本应
返回 FAIL 并指出磁盘阈值，但 7 个服务容器应保持 running。演练结束后恢复签署阈值并复跑 PASS。
