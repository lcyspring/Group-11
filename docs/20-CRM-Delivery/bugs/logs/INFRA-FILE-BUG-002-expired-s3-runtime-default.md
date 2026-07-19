# INFRA-FILE-BUG-002：本机默认文件客户端指向失效外部 S3

发现日期：2026-07-14。状态：Fixed。

## 现象

合同签署副本真实上传验收调用 `/admin-api/infra/file/upload` 返回系统异常。Server 日志确认
当前主文件客户端仍是初始化数据中的外部 S3 示例账号，服务端收到 S3 403，导致 CRM 合同附件、
跟进附件等所有上传入口不可用。

## 修复

Podman 运行 YAML 新增显式文件存储配置：

```yaml
file:
  storage_mode: database
  client_id: 4
  public_base_url: http://127.0.0.1:8080
```

`deploy.sh` 在 MySQL 兼容迁移后、Server 启动前选择数据库文件客户端并更新其公开域名。客户端编号和
域名不再隐藏在脚本常量或命令行参数中；命令行仍只接收 YAML 路径。URL 通过受限格式校验，客户端
编号必须为正整数。

此设置只影响 Podman 观察配置。生产环境必须在自己的 YAML 中明确选择可用的文件客户端，不能
继续继承演示 S3 凭据。
