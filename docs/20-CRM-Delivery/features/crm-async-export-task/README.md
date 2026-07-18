# CRM-FEATURE-060：CRM 异步导出任务与受控下载

状态：已完成并通过真实 HTTP 验收。分支：`develop`。完成日期：2026-07-18。

## 用户闭环

客户列表导出改为“提交后台任务 → 查看生成状态 → 申请短期令牌 → 下载结果”。导出任务中心展示
排队、运行、成功、失败和过期五种状态，活动任务自动轮询，失败任务保留有界诊断信息。用户不再因
大数据量 Excel 生成占用浏览器请求，也不能读取其他用户的任务。

## 安全与一致性

- 提交时冻结筛选条件及当时有权访问的客户 ID，不依赖后台执行时重新解释可变筛选；
- 提交、生成、令牌签发和下载均校验当前租户、任务所有人、对象存在性及对象导出权限；
- 结果写入 `crm-protected/export/{taskId}`，不复用公开文件路径；
- 下载令牌只保存 SHA-256，短期有效，并由条件更新原子消费；首次成功后同令牌立即失效；
- 任务过期后转为 `EXPIRED` 并删除受保护结果文件；多实例调度使用 Redisson 分布式锁；
- 单用户活动任务数、单任务行数、批量大小、保留时间和令牌 TTL 均由 YAML 显式限制。

## 接口与持久化

```text
POST /crm/export-task/customer
GET  /crm/export-task/page
GET  /crm/export-task/get
POST /crm/export-task/download-token
GET  /crm/export-task/download
```

任务表由 `database/migrations/new-crm-export-task.sql` 创建，迁移已进入 bootstrap 和 compatibility
manifest。运行配置位于 `crm_export_task` YAML 节点，字段说明见
`podman/config/YAML_FIELDS_ZH.md`。

## 验证入口

```bash
bash podman/compile.sh podman/config/test-crm-ubuntu-26.04.yaml
bash podman/compile.sh podman/config/verify-crm-customer-export-ubuntu-26.04.yaml
bash podman/compile.sh podman/config/check-web-types-ubuntu-26.04.yaml
bash podman/tests/acceptance/verify-crm-customer-export.sh podman/config/verify-crm-customer-export.example.yaml
```

命令行只指定 YAML 路径，依赖由 Ubuntu 26.04 容器运行时安装或复用 named volume，Host 不安装
项目依赖。
