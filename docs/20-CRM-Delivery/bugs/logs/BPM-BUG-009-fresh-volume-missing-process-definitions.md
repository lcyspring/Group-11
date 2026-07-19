# BPM-BUG-009 全新数据库卷缺少业务流程定义

## 问题现象

删除并重建数据库卷后，回款管理提交审批提示“流程定义不存在”。报销、合同、退款和 OA 出差也存在
相同风险。

## 原因分析

Flowable 已部署的模型和流程定义存放在 MySQL 业务卷中。`deploy.sh full` 会重新执行建表、基础数据和兼容
迁移，但业务审批模型由管理 API 创建，不属于静态建表 SQL，因此新卷中没有这些运行定义。

## 修复

- 重新创建并部署回款、报销、合同、退款和出差流程模型。
- 增加 `provision-bpm-models.sh <yaml>`，通过一个显式 YAML 顺序恢复全部受管模型。
- 单模型账号仍只保存在被 Git 忽略的 local YAML；聚合配置只引用路径，不复制密码。
- 全新数据卷部署完成后必须执行模型恢复，再进行业务提交审批验收。

## 验证

管理 API 已能查询以下有效流程定义：

- `crm-receivable-audit`
- `crm-reimbursement-audit`
- `crm-contract-audit`
- `crm-receivable-refund-audit`
- `oa_trip`
