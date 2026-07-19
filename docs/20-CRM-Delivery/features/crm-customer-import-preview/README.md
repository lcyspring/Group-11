# CRM 客户导入预检与字段映射

状态：已实现。分支：`develop`。完成日期：2026-07-17。

## 用户闭环

客户导入采用“上传文件 → 识别表头 → 调整字段映射 → 预检 → 确认导入”的两阶段流程。
预检只保存当前租户、当前用户可见的快照，不写客户业务表；确认时才复用既有客户导入服务创建或更新
客户、负责人历史和对象权限。

页面显示总行数、可创建数、可更新数、失败数及逐行结论。标准模板自动映射，自定义表头可映射到
客户名称、手机、电话、QQ、微信、邮箱、地区、详细地址、行业、等级、来源和备注。客户名称必须且
只能映射一次，同一文件列不能重复映射。

## 服务端契约

- `POST /crm/customer/import-preview`：解析文件并生成零业务写入的预检快照；
- `GET /crm/customer/import-preview/get`：读取本人在当前租户内的预检结果；
- `POST /crm/customer/import-preview/confirm`：按快照确认导入，重复确认返回首次结果；
- `POST /crm/customer/import`：保留为兼容入口，不作为新页面默认路径。

预检状态为 `PREVIEWED`、`CONFIRMED`、`EXPIRED`。文件内重复、数据库重复、必填、长度、邮箱、
字典和地区问题均落到具体行号；跨用户、跨租户及过期快照不可读取或确认。

## 显式配置与数据

运行 KDL 通过 `crm_customer_import.max_rows` 限制单次行数，通过
`crm_customer_import.preview_ttl_minutes` 控制确认有效期。字段说明见
`podman/config/KDL_FIELDS_ZH.md`。

持久化迁移为 `database/migrations/new-crm-customer-import-preview.sql`，并已进入 MySQL bootstrap 与
compatibility manifest。预检不保存原始 Excel 二进制，只保存规范化业务字段和逐行结论。

## 验证入口

```bash
bash podman/compile.sh podman/config/verify-crm-customer-import-ubuntu-26.04.kdl
bash podman/compile.sh podman/config/test-crm-ubuntu-26.04.kdl
bash podman/tests/acceptance/verify-crm-customer-import.sh podman/config/verify-crm-customer-import.example.kdl
```

命令行只指定 KDL 路径，依赖安装、测试和构建均在 Ubuntu 26.04 容器内完成。
