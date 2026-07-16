# Podman 配置目录中文索引

## 日常配置

| 类型 | 模板/本机文件 | 用途 |
|---|---|---|
| Server/Web 构建 | `build-ubuntu-26.04.yaml` | Ubuntu 26.04 全量构建 |
| Mall H5 构建 | `build-mall-h5-ubuntu-26.04.yaml` | 无图形 HBuilderX 构建 |
| 运行预检 | `runtime-local-check.yaml` | 不改变 Pod、镜像和卷 |
| 本机运行 | `runtime-local.yaml` | ignored，保存真实本地凭据和 full 模式 |
| 单项热替换 | `runtime-local-rebuild-*.yaml` | ignored，仅替换 Server/Web/Mall |
| 数据保护 | `database-backup-check.yaml` | 备份/恢复安全模板 |
| 编译镜像 | `build-image-archives-check.yaml` | 工具链镜像 check/save/load/push 模板 |
| CRM 性能基线 | `verify-crm-performance-baseline.example.yaml` | 只读并发负载与阈值共享模板 |

## 测试配置

`verify-*`、`test-*`、`check-*` 和 `bpm-provision-*` 均是结构化测试/验收资产。它们记录测试模块、
账号、端点、期望值和 Ubuntu 构建开关，供 Bug/功能回归复现，不作为日常部署入口。
性能基线先复制共享模板为 ignored 的 `verify-crm-performance-baseline-local.yaml`，再填写真实账号；
报告写入 CRM 独立证据目录。

新增配置时应优先复用现有协议，不再为一个布尔值增加命令行参数。完整字段见
[YAML_FIELDS_ZH.md](YAML_FIELDS_ZH.md)。
