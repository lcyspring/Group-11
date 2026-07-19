# Podman 低频运维入口

- `database/`：备份、恢复演练和显式数据集替换；
- `images/`：基础镜像离线归档及工具链镜像 save/load/push；
- `bpm/`：单模型与聚合 BPM 幂等 provision；
- `diagnostics/`：CRM 服务、容器、数据库和宿主指标诊断包。

所有入口只接收一个 KDL 路径。它们不属于日常三阶段命令，执行有状态操作前必须先使用对应
`check` 配置。
