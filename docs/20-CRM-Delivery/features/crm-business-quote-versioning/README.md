# CRM-FEATURE-044：商机报价版本与高级价格快照

- 分支：`develop`
- 状态：已实现
- 日期：2026-07-16

## 业务闭环

商机创建时同步生成 V1 报价草稿。草稿随商机产品编辑实时重算；客户确认后锁定为不可变版本，
如需调整必须填写原因并重开下一版本。赢单必须存在当前锁定报价，输单或无效则把当前报价冻结为
终止态。商机转合同只读取锁定报价，不接受客户端再次提交的产品、价格、币种或税率。

状态机为：

```text
DRAFT --lock--> LOCKED --reopen--> SUPERSEDED + next DRAFT
  |                 |
  +---- lose -------+---- invalid ----> TERMINATED
```

## 数据与金额契约

- `crm_business_quote` 保存版本、币种、汇率、折扣、未税/税额/含税和本位币金额；
- `crm_business_quote_item` 保存产品名称、编码、单位、分类、产品版本、目录价、成交价、数量和税率快照；
- `crm_business_quote_action_record` 保存创建、更新、锁定、重开和终止动作；
- 产品数量和全链路金额使用 `BigDecimal` 与 `decimal(24,6)`，支持小数数量；
- 产品主数据更新在事务行锁内递增版本，避免并发修改生成重复快照版本；
- 商机目录价始终由服务端 CRM 产品目录覆盖，客户端只可提交成交价和允许的税率；
- 合同来源报价、币种、汇率、税额、含税额及产品行均为不可变成交快照。

## 显式配置

`application.yaml` 的 `mitedtsm.crm.quote` 是唯一策略来源，包含：

- 策略版本、本位币、默认币种和兑本位币汇率；
- 允许税率和默认税率；
- 金额精度、舍入方式和单商机最大版本数。

管理端通过只读策略 API 获取同一份币种和税率选项，不在前端复制业务阈值。构建时遗留媒体源也由
Ubuntu 26.04 YAML 显式注入，命令行只接受配置文件路径。

## 管理端能力

- 商机表单支持币种、税率和小数数量；
- 商机详情展示当前报价、版本历史、金额拆分、产品快照和动作轨迹；
- 具备对象写权限及独立功能权限的用户才可锁定或重开；
- 锁定、终止和已结束商机不再显示可编辑入口；
- 中文、英文和阿拉伯文文案同步补齐。

## 交付入口

- 测试：`docs/20-CRM-Delivery/testing/crm-business-quote-versioning/`
- 运行证据：`docs/20-CRM-Delivery/runtime/yaml-podman-deployment/2026-07-16-crm-business-quote-versioning.md`
- 迁移：`database/migrations/new-crm-business-quote-version.sql`
- 真实验收：`podman/verify-crm-quote.sh` 与显式 YAML
