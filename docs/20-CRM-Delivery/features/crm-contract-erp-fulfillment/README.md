# CRM 合同到 ERP 履约订单

状态：已实现。分支：`develop`。日期：2026-07-16。

## 业务闭环

- CRM 合同保持唯一销售协议，ERP 销售订单只承接仓储履约；CRM 仅依赖 ERP 应用 API。
- 只有审批通过且存在有效签署事实的合同可由用户显式创建履约订单。
- CRM 客户、产品与 ERP 客户、产品通过独立一对一映射维护，不共享主键或直接访问对方 Mapper。
- 首次创建冻结合同版本、产品、价格、税率、币种、汇率和金额 JSON 快照及 SHA-256。
- CRM 履约表和 ERP 销售订单均设置租户级唯一键；失败重试复用原快照，跨域回写中断后可取回原 ERP 订单。
- 外部来源 ERP 订单禁止修改客户、产品和价格行，也禁止删除；审核、出库和退货流程保持可用。
- ERP 审核、出库和退货变化通过应用事件回传 CRM，并提供显式刷新作为补偿通道。
- 合同详情增加“ERP 履约”页签，展示阻塞原因、映射准备度、冻结版本、金额、状态和数量进度；另提供主数据映射维护页。

## 显式策略

`mitedtsm.crm.erp-fulfillment` YAML 配置声明启停、版本、来源标识、ERP 币种、允许来源币种、换算模式、六位精度、舍入规则、金额容差、错误长度和默认账户。命令行脚本只接受 YAML 路径。

默认策略把 CNY、USD、EUR 合同换算为 CNY ERP 订单，使用合同冻结汇率，不在代码中隐式读取在线汇率。

## 数据库对象

- `crm_erp_customer_mapping`
- `crm_erp_product_mapping`
- `crm_contract_fulfillment`
- `erp_sale_order` 外部来源、请求哈希和币种字段
- CRM 与 ERP 双侧来源/请求唯一键

迁移 `database/new/new-crm-erp-fulfillment.sql` 已连续执行两次通过。
