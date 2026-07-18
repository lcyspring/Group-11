# CRM-FEATURE-067：支付跨域集成规范

状态：规范已完成，自动集成未实现。分支：`develop`。日期：2026-07-18。

已形成支付与 CRM 回款/退款、OA 借款/报销、营销付费之间的聚合边界、稳定映射键、API/事件、错误码、
状态映射、权限矩阵、审计字段、签名和重放保护、失败补偿、跨租户隔离、YAML 草案及评审清单。

规范入口：`docs/20-CRM-Delivery/integration/payment-cross-domain/`。

本项关闭的是工作计划要求的“集成规范”缺口，不把它写成“支付自动集成已上线”。当前所有自动触发项
保持关闭，后续实现必须逐阶段通过 `testing/payment-cross-domain-integration-contract/` 门禁。

